package com.wechat.editor.utils

import java.net.URI

data class GitHubRepositoryTarget(
    val owner: String,
    val repo: String,
    val branch: String,
    val directoryPath: String
) {
    fun filePath(fileName: String): String {
        val cleanDirectory = directoryPath.trim('/').trim()
        return if (cleanDirectory.isBlank()) fileName else "$cleanDirectory/$fileName"
    }
}

object GitHubTargetParser {
    private const val DEFAULT_BRANCH = "main"

    fun parse(input: String, branchFallback: String = DEFAULT_BRANCH): GitHubRepositoryTarget? {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isBlank()) return null

        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            parseGitHubUrl(trimmed, branchFallback)
        } else {
            parseOwnerRepoPath(trimmed, branchFallback)
        }
    }

    private fun parseGitHubUrl(input: String, branchFallback: String): GitHubRepositoryTarget? {
        val uri = runCatching { URI(input) }.getOrNull() ?: return null
        val host = uri.host.orEmpty().lowercase()
        if (host != "github.com" && host != "www.github.com") return null

        val parts = uri.path.trim('/').split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return null

        val owner = parts[0]
        val repo = parts[1].removeSuffix(".git")
        if (owner.isBlank() || repo.isBlank()) return null

        if (parts.size >= 4 && parts[2] == "tree") {
            return GitHubRepositoryTarget(
                owner = owner,
                repo = repo,
                branch = parts[3].ifBlank { normalizedBranch(branchFallback) },
                directoryPath = parts.drop(4).joinToString("/")
            )
        }

        return GitHubRepositoryTarget(
            owner = owner,
            repo = repo,
            branch = normalizedBranch(branchFallback),
            directoryPath = parts.drop(2).joinToString("/")
        )
    }

    private fun parseOwnerRepoPath(input: String, branchFallback: String): GitHubRepositoryTarget? {
        val parts = input.trim('/').split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val owner = parts[0]
        val repo = parts[1].removeSuffix(".git")
        if (owner.isBlank() || repo.isBlank()) return null

        return GitHubRepositoryTarget(
            owner = owner,
            repo = repo,
            branch = normalizedBranch(branchFallback),
            directoryPath = parts.drop(2).joinToString("/")
        )
    }

    private fun normalizedBranch(value: String): String = value.trim().ifBlank { DEFAULT_BRANCH }
}
