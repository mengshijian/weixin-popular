package com.cootf.wechat.bean.scan.crud;

import com.cootf.wechat.bean.BaseResult;
import com.cootf.wechat.bean.scan.info.ActionInfo;


public class BrandInfoUpdate extends BaseResult {
    private ActionInfo action_info;

    public ActionInfo getAction_info() {
        return action_info;
    }

    public void setAction_info(ActionInfo action_info) {
        this.action_info = action_info;
    }
}
