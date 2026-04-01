import java.util.Properties

plugins {
    `maven-publish`
    signing
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

fun getProperty(name: String): String? =
    localProps.getProperty(name) ?: System.getenv(name)

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.findByName("release"))

                groupId = property("ZZ143_GROUP") as String
                artifactId = project.name
                version = property("ZZ143_VERSION") as String

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
        }

        repositories {
            maven {
                name = "sonatype"
                val releasesUrl = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                val snapshotsUrl = "https://ossrh-staging-api.central.sonatype.com/content/repositories/snapshots/"
                val isSnapshot = (property("ZZ143_VERSION") as String).endsWith("SNAPSHOT")
                url = uri(if (isSnapshot) snapshotsUrl else releasesUrl)

                credentials {
                    username = getProperty("OSSRH_USERNAME") ?: ""
                    password = getProperty("OSSRH_PASSWORD") ?: ""
                }
            }
        }
    }

    val signingKeyId = getProperty("SIGNING_KEY_ID")
    val signingKey = getProperty("SIGNING_KEY")

    if (signingKey != null && signingKey.isNotBlank()) {
        signing {
            val signingPassword = getProperty("SIGNING_PASSWORD") ?: ""
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            sign(publishing.publications["release"])
        }
    }
}
