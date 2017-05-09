package me.tatarka.silentsupport.lint.context

import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import org.jetbrains.uast.UElement

class RewriteIssueJavaContext(
        context: JavaContext,
        private val issueRewriter: RewriteIssueJavaContext.JavaIssueRewriter)
    : JavaContext(context.driver, context.project, context.mainProject, context.file) {

    init {
        compilationUnit = context.compilationUnit
        uastParser = context.uastParser
        uastFile = context.uastFile
    }

    override fun report(issue: Issue, element: UElement, location: Location?, message: String?) {
        issueRewriter.rewriteIssue(this, issue, element, location, message)?.let { newIssue ->
            issueRewriter.rewriteIssue(newIssue, location, message)?.let { newIssue ->
                super.report(newIssue, element, location, message)
            }
        }
    }

    override fun report(issue: Issue, location: Location?, message: String?) {
        issueRewriter.rewriteIssue(issue, location, message)?.let { newIssue ->
            super.report(newIssue, location, message)
        }
    }

    interface JavaIssueRewriter : IssueRewriter {
        fun rewriteIssue(context: JavaContext, issue: Issue, element: UElement, location: Location?, message: String?): Issue?
    }
}
