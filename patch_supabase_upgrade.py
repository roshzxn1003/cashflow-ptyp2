import re

# Update libs.versions.toml
with open('/app/applet/gradle/libs.versions.toml', 'r') as f:
    content = f.read()

content = content.replace('supabase = "2.6.1"', 'supabase = "3.1.0"')
content = content.replace('[libraries.supabase-gotrue]', '[libraries.supabase-auth]')
content = content.replace('name = "gotrue-kt"', 'name = "auth-kt"')

with open('/app/applet/gradle/libs.versions.toml', 'w') as f:
    f.write(content)

# Update build.gradle.kts
with open('/app/applet/app/build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace('implementation(libs.supabase.gotrue)', 'implementation(libs.supabase.auth)')

with open('/app/applet/app/build.gradle.kts', 'w') as f:
    f.write(content)

# Update Kotlin files
files_to_update = [
    '/app/applet/app/src/main/java/com/example/data/network/SupabaseClientConfig.kt',
    '/app/applet/app/src/main/java/com/example/data/network/SupabaseAuthService.kt'
]

for file in files_to_update:
    with open(file, 'r') as f:
        content = f.read()
    
    content = content.replace('io.github.jan.supabase.gotrue', 'io.github.jan.supabase.auth')
    
    with open(file, 'w') as f:
        f.write(content)

