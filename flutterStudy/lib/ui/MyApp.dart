import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_study/contract/flutter_contract.dart';
import 'package:flutter_study/ui/DemoItem.dart';

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(home: MyHomePage());
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel(FlutterContract.channelName);

  // 提供给 Android 调用的方法
  Future<String> getFlutterData(String input) async {
    return "Flutter 处理后的数据: $input";
  }

  @override
  void initState() {
    super.initState();

    // 设置方法调用处理器
    platform.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'getFlutterData':
          final String input = call.arguments as String;
          return await getFlutterData(input);
        case 'calculateSum':
          final List<int> numbers = List<int>.from(call.arguments as List);
          return numbers.reduce((a, b) => a + b);
        default:
          throw PlatformException(
            code: '未实现的方法',
            message: '方法 ${call.method} 未实现',
            details: null,
          );
      }
    });
  }

  Widget _columnTest() {
    Column(
      ///主轴居中,即是竖直向居中
      mainAxisAlignment: MainAxisAlignment.center,

      ///大小按照最小显示
      mainAxisSize: MainAxisSize.min,

      ///横向也居中
      crossAxisAlignment: CrossAxisAlignment.center,
      children: <Widget>[
        ///flex默认为1
        Expanded(flex: 2, child: Text("1111")),
        Expanded(child: Text("2222")),
      ],
    );
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(flex: 2, child: Text("1111")),
        Expanded(child: Text("2222")),
      ],
    );
  }

  Widget _containerTest() {
    return Container(
      margin: EdgeInsets.all(10.0),
      height: 120.0,
      width: MediaQuery.of(context).size.width,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.all(Radius.circular(4.0)),
        color: Colors.cyan,
        border: Border.all(color: Colors.black45, width: 0.3),
      ),
      alignment: Alignment.topCenter,
      child: Text("666666"),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Flutter Module'),
        backgroundColor: Colors.white,
      ),
      body: DemoItem(),
      backgroundColor: Colors.white,
    );
  }
}
