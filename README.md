## Overview
不同于CardView的阴影偏移，FakeCardView为View周围添加同尺寸阴影，使用方式大体相同。
[相关博客](http://www.jianshu.com/p/001cad7f43de)

## Getting started
### 添加依赖
```gradle
 dependencies {
    compile 'io.github.proton626.library.FakeCardView:library:1.0.0'
}
```
### xml中声明
```xml
 <io.github.proton626.library.FakeCardView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_centerInParent="true"
        app:cardCornerRadius="5dp"
        app:cardElevation="10dp"
        app:cardMaxElevation="10dp"
        app:cardPreventCornerOverlap="true">

        <ImageView
            android:layout_width="320dp"
            android:layout_height="200dp"
            android:scaleType="fitXY"
            android:src="@mipmap/cover" />
    </io.github.proton626.library.FakeCardView>

```
<br/>

**API21显示效果**

<div align="center">
<img src="fake_card_view_api21.png"/>
</div>
<br>

**API18显示效果**
<div align="center">
<img src="fake_card_view_api18.png"/>
</div>
<br/>

# License
This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
