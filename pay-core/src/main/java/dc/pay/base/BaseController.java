package dc.pay.base;


import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;


public class BaseController {

    protected  static  BeansWrapper wrapper = new BeansWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build();
    protected  static  TemplateHashModel staticModels = wrapper.getStaticModels();
    protected static TemplateHashModel useStaticPacker(String packname) {
        TemplateHashModel fileStatics = null;
        try {
            fileStatics = (TemplateHashModel) staticModels.get(packname);
        } catch (TemplateModelException e) {
            e.printStackTrace();
        }
        return fileStatics;
    };

}

//map.put("urlUtil", useStaticPacker("com.zxsd.test.util.UrlUtil"));  ${UrlUtil.getUrl("Nwenzhang.css")!}
//root.put("enums", BeansWrapper.getDefaultInstance().getEnumModels());  ${enums["java.math.RoundingMode"].UP}
//map.put("roundingModeEnums", useStaticPacker("java.math.RoundingMode"));  ${RoundingMode.UP}