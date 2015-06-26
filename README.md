# Android Screenshot Layer
An intuitive way to take screenshots in android using custom layout

## Screencast Demo
![](/screencast.gif)

### Layout Usage
Add the main layout as a child of this Layer to enable screenshots.
Swipe from left or right edges of the screen.
([activity_main.xml](/demo/src/main/res/layout/activity_main.xml))
```xml

<com.rajasharan.layout.ScreenshotLayer
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/rearrangeable_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    >

    <!-- Add main layout here -->

</com.rajasharan.layout.ScreenshotLayer>
```
