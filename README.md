# 用户手册
### 使用源代码
你需要添加百度地图相关的so包才能使用此程序。相关的包添加到jniLibs文件夹下面即可。本程序依赖于地图包进行可视化绘制。

### 简介

本软件基于 Anrdoid 和 Android 手机集成传感器开发，进行了 WIFI 和 PDR 室内定位的实验。本手册用于简单说明软件使用方法和用户界面构成。

#### WIFI定位功能

![wifi1](/media/wifi_positioning_1.png) ![wifi1](/media/wifi_positioning_2.png) ![choose_algorithm](/media/algorithm_pick.png)
- 添加结束接入点和采样点之后，采样点会被绘制到地图上，随着wifi强度信息的积累，定位的位置会随着用户的走动不断变化。
- 使用WIFI 定位功能之前，用户可以选择使用的算法。系统实现了几种基本算法：KNN(Euclidean \ Cosine) \ Bayes。用户可以使用不同的算法进行试验。

#### WIFI定位离线步骤
![add_ap](/media/adding_access_point.png) ![add_rp](/media/adding_reference_point.png) ![delete_rp](/media/deleting_reference_point.png)

- 使用WIFI 定位功能之前，用户需要采集当地的wifi指纹。
- 点击`+接入点`按钮，系统自动添加强度足够的SSID到数据库中，这些信号源参与定位比较。
- 点击`+参考点`按钮，进入添加参考点界面。用户可以拖动界面上的红色手柄调节参考点的地理位置。软件通过基站和GPS等自动给出估计的位置。
- 长按对应的参考点和接入点可以删除相对应的数据。

#### PDR定位功能

![pdr](/media/pdr.png)
- 用户将手机对准正前方稳定握持，用户点击开始之后，算法自动开始记录手机加速度数据。
- 用户的轨迹会被一根线记录在地图上。
- 通过拖动手柄调节起始位置。
- 通过拖动滑动条调节传感器磁偏角初始误差。
- 通过调节下面的滑动条调节步长和实际距离之间的比例关系。
