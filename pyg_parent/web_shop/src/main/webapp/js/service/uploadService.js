app.service("uploadService",function($http){
	
	this.uploadFile = function(){
		// 向后台传递数据:
		var formData = new FormData();
		// 向formData中添加数据:
		formData.append("file",file.files[0]);
		
		return $http({
			method:'post',
			url:'../upload/uploadFile.do',
			data:formData,
			headers:{'Content-Type':undefined} ,// 默认请求头是Content-Type :application/text  , 这里通过设置headers属性相当于改成application/json
			transformRequest: angular.identity // 相当于在html的form标签中加入content-type=multipart/form-data, 这里使用的是angularjs序列化上传的文件
		});
	}
	
});