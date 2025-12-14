## Java 版本兼容性问题临时解决方案

由于系统安装了 Java 25，但 Kotlin 编译器和 Gradle 插件尚未完全兼容，请按以下步骤操作：

### 方案 1：使用 Android Studio 内置 JDK（推荐）

1. 打开 **File** → **Settings**（Windows）或 **Preferences**（Mac）
2. 导航到 **Build, Execution, Deployment** → **Build Tools** → **Gradle**
3. 在 **Gradle JDK** 下拉菜单中选择：
   - `Embedded JDK (jbr-17)` 或
   - `Android Studio Java home`
4. 点击 **OK** 并重新构建项目

### 方案 2：设置 JAVA_HOME 环境变量

在项目根目录创建 `local.properties` 或编辑现有文件，添加：

```properties
# 指向 JDK 17 的路径（示例，请根据实际情况修改）
# Windows:
# org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr
# 
# Mac:
# org.gradle.java.home=/Applications/Android Studio.app/Contents/jbr/Contents/Home
```

### 方案 3：临时使用命令行指定 Java 版本

```bash
# 如果有 JDK 17 安装在其他位置
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
./gradlew assembleDebug
```

### 临时解决方法（不推荐用于生产）

如果只是想快速测试，可以暂时降低 compileSdk：

```kotlin
// build.gradle.kts (Module: app)
android {
    compileSdk = 34  // 降级到 34，会有警告但能编译
    // ...
}
```

**注意**：这会导致 18 个 AAR metadata 警告，但不影响运行。
