with open('/app/applet/gradle/libs.versions.toml', 'r') as f:
    content = f.read()

content = content.replace('supabase = "3.0.1"', 'supabase = "2.6.1"')
content = content.replace('ktor = "3.0.1"', 'ktor = "2.3.12"')

with open('/app/applet/gradle/libs.versions.toml', 'w') as f:
    f.write(content)
