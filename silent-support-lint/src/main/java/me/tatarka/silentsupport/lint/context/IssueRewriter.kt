package me.tatarka.silentsupport.lint.context

import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location

interface IssueRewriter {
    fun rewriteIssue(issue: Issue, location: Location?, message: String?): Issue?
}
