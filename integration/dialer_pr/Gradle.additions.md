Add DialerBridge module or sources and depend on it:

settings.gradle:
```
include(':DialerBridge')
project(':DialerBridge').projectDir = file('../packages/DialerBridge')
```

app/build.gradle:
```
dependencies {
    implementation project(':DialerBridge')
}
```