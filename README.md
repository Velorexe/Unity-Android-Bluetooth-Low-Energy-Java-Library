# Unity Android Bluetooth Low Energy Java Library


<p align="center">
    The Java library that the Unity Android Bluetooth Low Energy Plugin binds to<br>
    <i>I should have picked a shorter title</i>
</p>

<p align="center">
    <img src="https://i.imgur.com/fL3ybma.png" style="width:40%;">
</p>

## Features
This repository is to open-source the library that's used in my [Unity Android Bluetooth Low Energy](https://github.com/Velorexe/Unity-Android-Bluetooth-Low-Energy) plugin under the `Assets/Plugins/Android/unityandroidble.jar`. To not make everything secret or closed-sourced, this repository contains the Java side of things, for transparency sake, but also for people who want to expand on the project and add other custom commands.

## What You Need
In order to properly compile this to a working jar, you need two things:
* The `unity.jar` Java library that's located somewhere in _your_ Unity Editor's folder
* A `Gradle` script that compiles your code to a Java library

The `unity.jar` is something you need to find yourself, then place in the `libs` folder of your Android project. The `Gradle` script is something I've added below and needs to be added to your `build.gradle`.

```gradle
task createJar(type: Copy) {
    from('build/intermediates/compile_app_classes_jar/release/')
    into('libs/')
    include('classes.jar')
    rename('classes.jar', 'unityandroidble.jar')
}
```

## Contact
If you need any information, have questions about the project or found any bugs in this project, please create a new `Issue` and I'll take a look at it! If you've got more pressing questions or questions that aren't related to create an Issue for, you can contact with the methods below.

* Discord: Velorexe#8403
* Email: degenerexe.code@gmail.com
