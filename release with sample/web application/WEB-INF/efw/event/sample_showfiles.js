var sample_showfiles={};
sample_showfiles.name="ファイル表示";
sample_showfiles.outOfLogin=true;
sample_showfiles.paramsFormat={};
sample_showfiles.fire=function(params){
	var data1=file.listFiles("");//storageフォルダのファイルを全部リストする
	var data2=file.listFolders("");//storageフォルダのファイルを全部リストする
	var data=(new Record(data1.concat(data2)))
				.map({"name":"name"})
				.getArray();
	
	return (new Result())
		.runat("#cmb_download")
		.remove("option")
		.append("<option value=\"{name}\">{name}")//{lastModified} {length}
		.withdata(data);
};