package com.cootf.wechat.bean.scan.info;

import com.cootf.wechat.bean.scan.infolist.ModuleList;

import java.util.List;

public class ModuleInfo {
    private List<ModuleList> module_list;

    public List<ModuleList> getModule_list() {
        return module_list;
    }

    public void setModule_list(List<ModuleList> module_list) {
        this.module_list = module_list;
    }
}
