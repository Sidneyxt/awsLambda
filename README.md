clone 项目到本地到项目的根目录
mvn clean install
./rmJarFile.sh
target中的aws-lambda-1.0-SNAPSHOT.jar 的jar 即可上传至aws lambda服务,配置s3 ObjectCreated事件通知
[相关文档](http://docs.aws.amazon.com/zh_cn/lambda/latest/dg/welcome.html)
