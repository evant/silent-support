package me.tatarka.silentsupport.lint

import com.android.tools.lint.detector.api.Issue

class IssueRegistry : com.android.tools.lint.client.api.IssueRegistry() {
    override fun getIssues(): List<Issue> = listOf(SilentSupportApiDetector.UNSUPPORTED)
}
