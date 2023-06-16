package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IVoucherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  Front-end controller
 * </p>
 *
 * @author Josephinesss
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    /**
     * newly created normal coupon
     * @param voucher coupon info
     * @return coupon id
     */
    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * newly created limited coupon
     * @param voucher coupon info, including limited version info
     * @return coupon id
     */
    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * search coupon list of shop
     * @param shopId shop id
     * @return coupon list
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }

    @DeleteMapping("{id}")
    public void deleteSeckillById(@PathVariable("id") long voucherId){
        voucherService.deleteSeckillById(voucherId);
    }
}
