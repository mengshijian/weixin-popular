package com.cootf.wechat.bean.message;

import java.util.List;

import com.cootf.wechat.bean.BaseResult;

public class GetAllPrivateTemplateResult extends BaseResult{

	private List<PrivateTemplate> template_list;

	public List<PrivateTemplate> getTemplate_list() {
		return template_list;
	}

	public void setTemplate_list(List<PrivateTemplate> template_list) {
		this.template_list = template_list;
	}
}
