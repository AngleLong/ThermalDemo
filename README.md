之前写过一篇关于Android增量更新的文章！关于增量更新其实还是有很多弊端的！首先，用户可以感知更新，其次，用户如果选择不去安装的话，就没有办法实现相应的更新操作！所以趁着最近工作不忙，研究了一下热更新！怎么说呢？可以让用户在无感知的情况下修复相应的bug！挺爽的！

# 本文知识点
- 阿里热更新hotfix的集成
- 阿里热更新hotfix的使用
- 阿里热更新hotfix的常见问题

> 为什么选择阿里的hotfix作为热更新呢？网上有很多人对比过这个！所以我在这边也就不献丑了！我使用阿里hotfix的主要原因是在调研的时候看到如下的对比：

![d69f6cccdc751a13215a8773ad648b57.png](evernotecid://15F9DE32-0B0C-4597-BAC2-265E889B9046/appyinxiangcom/25209387/ENResource/p11)

## 阿里热更新hotfix的集成

### 项目中的代码集成
> 这里先丢一个[官网链接](https://www.aliyun.com/product/hotfix?spm=a3c0d.7662652.1998907816.8.100abe48Qy3zxU)，其实关于hotfix的集成还是很简单的，基本上按照说明文档一顿复制粘贴就好了！
> 这里注意几点内容：
> - 这个框架超过一定限制是收费的！！！
> - 采用稳健的接入方式！
> - 下载的json文件和AndroidManifest.xml中设置的meta-data的对应关系

这里直接把[官方的集成文档的地址](https://help.aliyun.com/document_detail/61082.html?spm=a2c4g.11174283.3.6.2f5c30c3SUVnos)放到这里了，没有什么技术含量！

这了我直接把SophixStubApplication的类粘到这里大家看一下！

```java
/**
 * Sophix入口类，专门用于初始化Sophix，不应包含任何业务逻辑。
 * 此类必须继承自SophixApplication，onCreate方法不需要实现。
 * 此类不应与项目中的其他类有任何互相调用的逻辑，必须完全做到隔离。
 * AndroidManifest中设置application为此类，而SophixEntry中设为原先Application类。
 * 注意原先Application里不需要再重复初始化Sophix，并且需要避免混淆原先Application类。
 * 如有其它自定义改造，请咨询官方后妥善处理。
 */
public class SophixStubApplication extends SophixApplication {
    private final String TAG = "SophixStubApplication";

    private final String hotfixId = "27695217-1";
    private final String hotfixappKey = "2f9f91e00e3f72a028a24be8182947a1";

    static SophixStubApplication instance;

    // 此处SophixEntry应指定真正的Application，并且保证RealApplicationStub类名不被混淆。
    @Keep
    @SophixEntry(APP.class)
    static class RealApplicationStub {
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//         如果需要使用MultiDex，需要在此处调用。
//         MultiDex.install(this);
        initSophix();
    }

    private void initSophix() {
        String appVersion = "0.0.0";
        try {
            appVersion = this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
        }
        final SophixManager instance = SophixManager.getInstance();
        instance.setContext(this)
                .setAppVersion(appVersion)
                .setSecretMetaData(hotfixId, hotfixappKey, getString(R.string.hotfixrsaSecret))
                .setEnableDebug(true)
                .setEnableFullLog()
                .setPatchLoadStatusStub(new PatchLoadStatusListener() {
                    @Override
                    public void onLoad(final int mode, final int code, final String info, final int handlePatchVersion) {
                        Log.e(TAG, "修复模式：" + mode);
                        Log.e(TAG, "修复回调code：" + code);
                        Log.e(TAG, "修复信息：" + info);
                        Log.e(TAG, "修复版本：" + handlePatchVersion);

                        // 补丁加载回调通知
                        if (code == PatchStatus.CODE_LOAD_SUCCESS) {
                            // 表明补丁加载成功
                            Log.e(TAG, "表明补丁加载成功");
                        } else if (code == PatchStatus.CODE_LOAD_RELAUNCH) {
                            // 表明新补丁生效需要重启. 开发者可提示用户或者强制重启;
                            // 建议: 用户可以监听进入后台事件, 然后应用自杀
                            Log.e(TAG, "表明新补丁生效需要重启. 开发者可提示用户或者强制重启");
                        } else {
                            // 其它错误信息, 查看PatchStatus类说明
                            Log.e(TAG, "其它错误信息, 查看PatchStatus类说明");
                        }
                    }
                }).initialize();
    }

    public static SophixStubApplication getInstance() {
        return instance;
    }

    public void queryAndLoadNewPatch() {
        SophixManager.getInstance().queryAndLoadNewPatch();
    }
}
```
**这里注意一个问题：hotfixId这个注意一下就好了！其实可以在代码中获取相应的一些配置，但是这里建议在代码中配置，这样能更好的控制！**

建议首次集成的时候，把相应的错误码都打印出来，因为这样便于你对框架整体的理解！在你使用的地方就可以直接初始化，我这里直接是在相应的Application中进行相应的查询是否有app的更新和迭代！

```
SophixManager.getInstance().queryAndLoadNewPatch();//查询是否有新的补丁
```

这些设置完成之后，基本上在项目中的代码就写完了！剩下的就是相应的包上传了！这个才是集成的关键！

### 生成补丁包
> 关于生成补丁包打开这个[网址](https://help.aliyun.com/document_detail/93826.html?spm=a2c4g.11186623.2.10.7ca35b84Y96MKJ)然后下载对应的工具，其实我觉得这个工具就是做拆分包的，这样就能减少APP的体积了！

按照上面的说明直接获取相应的Patch，这里有一点需要注意一下，就是那个高级里面有一个是否冷启动的选项，名字可能不对(理解就好)。这个选项注意一下，因为有的时候可能它无法判断到底时候冷启动还是其它启动！所以这个选择还是要慎重！如果你签名的时候使用了打包文件，那么在工具中也可以设置！

![6f38be6c79a62342a6a93be73f8ee3be.png](evernotecid://15F9DE32-0B0C-4597-BAC2-265E889B9046/appyinxiangcom/25209387/ENResource/p15)

![f1d1338f1ace0fc9fb88d71416f53d8b.png](evernotecid://15F9DE32-0B0C-4597-BAC2-265E889B9046/appyinxiangcom/25209387/ENResource/p16)

上面是工具的两张截图！基本上的要点都说完了，其他的没有什么好说的！按照流程一步一步来就好了！

### 设置相应的补丁包

> 吐槽一下，阿里的东西是实在太多了，找起来真的费劲！所以这里给大家贴张图片！

![d8b1d60d581e740ca45d4a294073d059.png](evernotecid://15F9DE32-0B0C-4597-BAC2-265E889B9046/appyinxiangcom/25209387/ENResource/p17)

![d7da538eb17267e4e657959255767bd7.png](evernotecid://15F9DE32-0B0C-4597-BAC2-265E889B9046/appyinxiangcom/25209387/ENResource/p18)

然后进入到对应的项目页面设置相应的补丁！直接上传就好了，里面有一个二维码，可以下载应用。然后进行充分测试之后在进行发布！


之后我就各种断点这种尝试，还是可以的！这里附上[Android接入问题](https://help.aliyun.com/knowledge_list/51422.html?spm=a2c4g.11186623.6.585.2c4839976U7j8o)有什么问题可以先上上面去找一下，基本上都能找到!

--- 
好了今天的问题就到这里！拜🤗