with open('/app/applet/app/build.gradle.kts', 'r') as f:
    content = f.read()
    
content = content.replace("implementation(libs.ktor.client.android)", "implementation(libs.ktor.client.android)\n  implementation(libs.ktor.client.core)\n")

with open('/app/applet/app/build.gradle.kts', 'w') as f:
    f.write(content)
