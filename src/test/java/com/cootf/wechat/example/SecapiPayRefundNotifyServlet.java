package com.cootf.wechat.example;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cootf.wechat.bean.paymch.MchBaseResult;
import com.cootf.wechat.bean.paymch.RefundNotifyReqInfo;
import com.cootf.wechat.bean.paymch.SecapiPayRefundNotify;
import com.cootf.wechat.support.ExpireKey;
import com.cootf.wechat.support.expirekey.DefaultExpireKey;
import com.cootf.wechat.util.PayUtil;
import com.cootf.wechat.util.StreamUtils;
import com.cootf.wechat.util.XMLConverUtil;

/**
 * 退款回调通知
 * 
 * @author mengsj
 *
 */
public class SecapiPayRefundNotifyServlet extends HttpServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String key; // mch key

	// 重复通知过滤
	private static ExpireKey expireKey = new DefaultExpireKey();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// 获取请求数据
		String xmlData = StreamUtils.copyToString(request.getInputStream(), Charset.forName("utf-8"));
		// 转换数据对象
		SecapiPayRefundNotify refundNotify = XMLConverUtil.convertToObject(SecapiPayRefundNotify.class, xmlData);

		// 退款通知成功
		if (refundNotify != null && "SUCCESS".equals(refundNotify.getReturn_code())) {
			// 解密数据 req_info
			RefundNotifyReqInfo refundNotifyReqInfo = PayUtil.decryptRefundNotifyReqInfo(refundNotify.getReq_info(),
					key);
			if (refundNotifyReqInfo == null) {
				MchBaseResult baseResult = new MchBaseResult();
				baseResult.setReturn_code("FAIL");
				baseResult.setReturn_msg("ERROR");
				response.getOutputStream().write(XMLConverUtil.convertToXML(baseResult).getBytes());
				return;
			}
			// 业务处理标记检查
			if (!expireKey.exists("WX_REFUND_NOTIFY" + refundNotifyReqInfo.getRefund_id())) {
				// 添加业务处理标记
				expireKey.add("WX_REFUND_NOTIFY" + refundNotifyReqInfo.getRefund_id(), 60);

				// TODO 添加业务代码，修改退款申请状态
			}
		}
		MchBaseResult baseResult = new MchBaseResult();
		baseResult.setReturn_code("SUCCESS");
		baseResult.setReturn_msg("OK");
		response.getOutputStream().write(XMLConverUtil.convertToXML(baseResult).getBytes());
	}

}
