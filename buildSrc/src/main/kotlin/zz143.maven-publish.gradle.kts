import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    configure(AndroidSingleVariantLibrary(variant = "release", sourcesJar = true))

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    val signingKey = providers.environmentVariable("SIGNING_KEY")
        .orElse(providers.gradleProperty("SIGNING_KEY"))
        .orNull
    if (!signingKey.isNullOrBlank()) {
        signAllPublications()
    }

    coordinates(
        groupId = property("ZZ143_GROUP") as String,
        artifactId = project.name,
        version = property("ZZ143_VERSION") as String
    )

    pom {
        name.set(project.name)
        description.set("Session replay that replays back — an Android SDK that learns user patterns and automates repetitive workflows.")
        url.set("https://github.com/jeremiahddavid/zz143")

        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }

        developers {
            developer {
                id.set("jeremiahddavid")
                name.set("Jeremiah David")
                email.set("jeremiahddavid@users.noreply.github.com")
            }
        }

        scm {
            url.set("https://github.com/jeremiahddavid/zz143")
            connection.set("scm:git:git://github.com/jeremiahddavid/zz143.git")
            developerConnection.set("scm:git:ssh://github.com/jeremiahddavid/zz143.git")
        }
    }
}
