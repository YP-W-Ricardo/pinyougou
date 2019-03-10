//服务层
app.service('brandService',function($http){

	//增加 
	this.brandadd=function(entity){
		return  $http.post('../brand/add.do?',entity );
	}

    //模板页面回显
    this.selectOptionList = function(){
        return $http.get("../brand/selectOptionList.do");
    }

    this.selectOptionList = function(){
        return $http.get("../brand/selectOptionList.do");
    }

});
