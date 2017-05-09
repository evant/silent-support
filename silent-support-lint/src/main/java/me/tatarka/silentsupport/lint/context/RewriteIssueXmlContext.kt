package me.tatarka.silentsupport.lint.context

import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.XmlContext

import org.w3c.dom.Node

class RewriteIssueXmlContext(
        context: XmlContext,
        private val issueRewriter: IssueRewriter)
    : XmlContext(context.driver, context.project, context.mainProject, context.file, context.resourceFolderType, context.parser) {

    override fun report(issue: Issue, node: Node?, location: Location?, message: String?, quickfixData: Any?) {
        issueRewriter.rewriteIssue(issue, location, message)?.let { newIssue ->
            super.report(newIssue, node, location, message, quickfixData)
        }
    }

    override fun report(issue: Issue, location: Location, message: String?) {
        issueRewriter.rewriteIssue(issue, location, message)?.let { newIssue ->
            super.report(newIssue, location, message)
        }
    }
}
