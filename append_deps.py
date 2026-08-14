import re

with open('/app/applet/app/build.gradle.kts', 'r') as f:
    content = f.read()

deps = """
  implementation(libs.supabase.postgrest)
  implementation(libs.supabase.gotrue)
  implementation(libs.supabase.realtime)
  implementation(libs.ktor.client.android)
"""

content = content.replace("dependencies {", "dependencies {" + deps)

with open('/app/applet/app/build.gradle.kts', 'w') as f:
    f.write(content)
