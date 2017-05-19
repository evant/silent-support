package me.tatarka.silentsupport.lint

import com.android.builder.model.AndroidLibrary
import com.android.tools.lint.checks.ApiDetector
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.android.tools.lint.detector.api.LintUtils.getInternalMethodName
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiSuperExpression
import com.intellij.psi.util.PsiTreeUtil
import me.tatarka.silentsupport.SupportCompat
import me.tatarka.silentsupport.SupportMetadataProcessor
import me.tatarka.silentsupport.lint.context.IssueRewriter
import me.tatarka.silentsupport.lint.context.RewriteIssueJavaContext
import me.tatarka.silentsupport.lint.context.RewriteIssueXmlContext
import org.jetbrains.uast.*
import org.jetbrains.uast.util.isMethodCall
import org.w3c.dom.Attr
import org.w3c.dom.Element

class SilentSupportApiDetector : ApiDetector() {

    private val xmlIssueRewriter = SilentSupportIssueRewriter()
    private val javaIssueRewriter = JavaIssueRewriter()
    private var supportApiDatabase: ApiLookup? = null

    override fun beforeCheckProject(context: Context) {
        super.beforeCheckProject(context)
        supportApiDatabase = loadSupportApiDatabase(context)
    }

    private fun loadSupportApiDatabase(context: Context): ApiLookup? {
        val variant = context.mainProject.currentVariant ?: return null
        val deps = variant.mainArtifact.dependencies.libraries
        val supportCompat = findSupportCompat(deps) ?: return null

        val dir = context.client.getCacheDir(true)
        val processor = SupportMetadataProcessor(supportCompat, dir)
        val file = processor.metadataFile
        if (!file.exists()) {
            processor.process()
        }
        return ApiLookup.create(context.client, file, true)
    }

    private fun findSupportCompat(deps: Collection<AndroidLibrary>): SupportCompat? {
        for (dep in deps) {
            val rc = dep.resolvedCoordinates
            if ("com.android.support" == rc.groupId && "support-compat" == rc.artifactId) {
                return SupportCompat(dep.jarFile, rc.version)
            }
            val result = findSupportCompat(dep.libraryDependencies)
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun checkSupport(owner: String, name: String, desc: String): Boolean {
        return supportApiDatabase?.let { supportApiDatabase ->
            val compatClassName = owner.replaceFirst("android/".toRegex(), "android/support/v4/") + "Compat"
            val newDesc = desc.replace("(", "(L$owner;")
            val compatVersion = supportApiDatabase.getCallVersion(compatClassName, name, newDesc, true)
            return compatVersion >= 0
        } ?: false
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        super.visitAttribute(RewriteIssueXmlContext(context, xmlIssueRewriter), attribute)
    }

    override fun visitElement(context: XmlContext, element: Element) {
        super.visitElement(RewriteIssueXmlContext(context, xmlIssueRewriter), element)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return super.createUastHandler(RewriteIssueJavaContext(context, javaIssueRewriter))
    }

    private open class SilentSupportIssueRewriter : IssueRewriter {
        override fun rewriteIssue(issue: Issue, location: Location?, message: String?, quickFixData: LintFix?): Issue? {
            if (ApiDetector.UNSUPPORTED.id == issue.id) {
                return UNSUPPORTED
            } else {
                return issue
            }
        }
    }

    private inner class JavaIssueRewriter : SilentSupportIssueRewriter(), RewriteIssueJavaContext.JavaIssueRewriter {
        override fun rewriteIssue(context: JavaContext, issue: Issue, element: UElement, location: Location?, message: String?): Issue? {
            if (element is UCallExpression) {
                val expression = element
                val method = element.resolve()
                val containingClass = method!!.containingClass
                val evaluator = context.evaluator
                val name = getInternalMethodName(method)
                val owner = evaluator.getInternalName(containingClass!!)
                val desc = evaluator.getInternalDescription(method, false, false)
                val receiver: UExpression?
                if (expression.isMethodCall()) {
                    receiver = expression.receiver
                    if (receiver != null && receiver !is UThisExpression && receiver !is PsiSuperExpression) {
                        val type = receiver.getExpressionType()
                        if (type is PsiClassType) {
                            val expressionOwner = evaluator.getInternalName((type as PsiClassType?)!!)
                            if (expressionOwner != null && expressionOwner != owner) {
                                if (checkSupport(expressionOwner, name, desc)) {
                                    return null
                                }
                            }
                        }
                    } else {
                        // Unqualified call; need to search in our super hierarchy
                        var cls = expression.getContainingClass()
                        if (receiver is UThisExpression || receiver is USuperExpression) {
                            val pte = receiver as UInstanceExpression?
                            val resolved = pte!!.resolve()
                            if (resolved is PsiClass) {
                                cls = resolved as PsiClass?
                            }
                        }

                        while (cls != null) {
                            if (cls is PsiAnonymousClass) {
                                // If it's an unqualified call in an anonymous class, we need to
                                // rely on the resolve method to find out whether the method is
                                // picked up from the anonymous class chain or any outer classes
                                var found = false
                                val anonymousBaseType = cls.baseClassType
                                val anonymousBase = anonymousBaseType.resolve()
                                if (anonymousBase != null && anonymousBase
                                        .isInheritor(containingClass, true)) {
                                    cls = anonymousBase
                                    found = true
                                } else {
                                    val surroundingBaseType = PsiTreeUtil
                                            .getParentOfType(cls, PsiClass::class.java, true)
                                    if (surroundingBaseType != null && surroundingBaseType
                                            .isInheritor(containingClass, true)) {
                                        cls = surroundingBaseType
                                        found = true
                                    }
                                }
                                if (!found) {
                                    break
                                }
                            }
                            val expressionOwner = evaluator.getInternalName(cls)
                            if (expressionOwner == null || "java/lang/Object" == expressionOwner) {
                                break
                            }
                            if (checkSupport(expressionOwner, name, desc)) {
                                return null
                            }
                            cls = cls.superClass
                        }
                    }
                }
            }
            return issue
        }
    }

    companion object {
        /**
         * Accessing an unsupported API
         */
        @JvmStatic
        val UNSUPPORTED = Issue.create(
                "NewApiSupport",
                ApiDetector.UNSUPPORTED.getBriefDescription(TextFormat.RAW),
                ApiDetector.UNSUPPORTED.getExplanation(TextFormat.RAW),
                ApiDetector.UNSUPPORTED.category,
                ApiDetector.UNSUPPORTED.priority,
                ApiDetector.UNSUPPORTED.defaultSeverity,
                Implementation(
                        SilentSupportApiDetector::class.java,
                        ApiDetector.UNSUPPORTED.implementation.scope,
                        *ApiDetector.UNSUPPORTED.implementation.analysisScopes
                )
        )
    }
}
