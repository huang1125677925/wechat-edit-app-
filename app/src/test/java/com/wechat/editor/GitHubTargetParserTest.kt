package com.wechat.editor

import com.wechat.editor.utils.GitHubTargetParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubTargetParserTest {

    @Test
    fun `parses github tree url with directory`() {
        val target = GitHubTargetParser.parse(
            "https://github.com/octocat/hello-world/tree/main/articles/wechat",
            "dev"
        )

        requireNotNull(target)
        assertEquals("octocat", target.owner)
        assertEquals("hello-world", target.repo)
        assertEquals("main", target.branch)
        assertEquals("articles/wechat", target.directoryPath)
        assertEquals("articles/wechat/post.md", target.filePath("post.md"))
    }

    @Test
    fun `parses owner repo path with fallback branch`() {
        val target = GitHubTargetParser.parse("octocat/hello-world/articles", "drafts")

        requireNotNull(target)
        assertEquals("octocat", target.owner)
        assertEquals("hello-world", target.repo)
        assertEquals("drafts", target.branch)
        assertEquals("articles", target.directoryPath)
    }

    @Test
    fun `rejects non github urls`() {
        assertNull(GitHubTargetParser.parse("https://example.com/octocat/hello-world"))
    }
}
