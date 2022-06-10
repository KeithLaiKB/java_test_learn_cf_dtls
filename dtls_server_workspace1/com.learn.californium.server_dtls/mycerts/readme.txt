you can put some certs in this folder or sub folder(if you create) 


server中 

keystore 生成后 cer
然后用cer 生成 truststore

但是在这程序当中 
	它不会用到client的 cer 和 truststore
	它暂时只会用 keystore


这里myown是以前2.6.0版本的


new是新版本需要用到 命令行ssl的