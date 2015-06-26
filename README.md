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
    android:id="@+id/layer"
    android:layout_height="match_parent"
    android:layout_width="match_parent"
    >

    <!-- Add main layout here -->

</com.rajasharan.layout.ScreenshotLayer>
```

**Use ScreenshotListener for success/error messages**
```java

@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
setContentView(R.layout.activity_main);
root = (ScreenshotLayer) findViewById(R.id.layer);
root.setScreenshotListener(new ScreenshotLayer.ScreenshotListener() {
        @Override
        public void onScreenshotSaved(String path) {
            Toast.makeText(MainActivity.this, "Screenshot saved in " + path, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onScreenshotError(String reason) {
            Toast.makeText(MainActivity.this, "Screenshot failed: " + reason, Toast.LENGTH_SHORT).show();
        }
    });
}

```
