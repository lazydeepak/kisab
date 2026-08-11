plugins {
    id("com.android.application") version "9.3.0" apply false
}

subprojects {
    val githubActor = providers.gradleProperty("gpr.user")
        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
        .orElse("x-access-token")
        .get()
    val githubToken = providers.gradleProperty("gpr.token")
        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
        .orElse("")
        .get()

    repositories {
        google()
        mavenCentral()
        maven {
            name = "githubPackagesFoundation"
            url = uri("https://maven.pkg.github.com/lazydeepak/susankhya-app-foundation")
            credentials {
                username = githubActor
                password = githubToken
            }
        }
    }
}
