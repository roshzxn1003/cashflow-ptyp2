with open('/app/applet/gradle/libs.versions.toml', 'r') as f:
    content = f.read()

content = content.replace("[versions]\n", "[versions]\nkotlinxSerialization = \"1.7.3\"\n")
content += """
[libraries.kotlinx-serialization-json]
group = "org.jetbrains.kotlinx"
name = "kotlinx-serialization-json"
version.ref = "kotlinxSerialization"

[plugins.kotlin-serialization]
id = "org.jetbrains.kotlin.plugin.serialization"
version.ref = "kotlin"
"""

with open('/app/applet/gradle/libs.versions.toml', 'w') as f:
    f.write(content)

with open('/app/applet/app/build.gradle.kts', 'r') as f:
    build_content = f.read()

build_content = build_content.replace("plugins {", "plugins {\n  alias(libs.plugins.kotlin.serialization)\n")
build_content = build_content.replace("dependencies {", "dependencies {\n  implementation(libs.kotlinx.serialization.json)\n")

with open('/app/applet/app/build.gradle.kts', 'w') as f:
    f.write(build_content)
