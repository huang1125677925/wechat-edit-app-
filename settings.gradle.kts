import java.util.Properties

// Cursor / CI often expose the SDK only via ANDROID_HOME / ANDROID_SDK_ROOT. Gradle's Android
// plugin reads sdk.dir from local.properties; without it, `./gradlew lint` fails with
// "SDK location not found". Seed local.properties from the env when sdk.dir is unset.
val localPropsFile = rootDir.resolve("local.properties")
val props = Properties()
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { props.load(it) }
}
val sdkFromFile = props.getProperty("sdk.dir")?.trim()?.takeIf { it.isNotEmpty() }
val sdkFromEnv = System.getenv("ANDROID_SDK_ROOT")?.trim()?.takeIf { it.isNotEmpty() }
    ?: System.getenv("ANDROID_HOME")?.trim()?.takeIf { it.isNotEmpty() }
if (sdkFromFile == null && sdkFromEnv != null) {
    props.setProperty("sdk.dir", sdkFromEnv)
    localPropsFile.outputStream().use {
        props.store(it, "sdk.dir auto-set from ANDROID_SDK_ROOT / ANDROID_HOME when unset in local.properties")
    }
}

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WeChatEditor"
include(":app")
