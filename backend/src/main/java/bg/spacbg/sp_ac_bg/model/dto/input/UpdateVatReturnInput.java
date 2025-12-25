package bg.spacbg.sp_ac_bg.model.dto.input;

import java.math.BigDecimal;

public class UpdateVatReturnInput {
    private BigDecimal vatToPay;
    private BigDecimal vatToRefund;
    private BigDecimal effectiveVatToPay;
    private BigDecimal vatForDeduction;
    private BigDecimal vatRefundArt92;
    private String notes;

    // Getters and Setters
    public BigDecimal getVatToPay() {
        return vatToPay;
    }

    public void setVatToPay(BigDecimal vatToPay) {
        this.vatToPay = vatToPay;
    }

    public BigDecimal getVatToRefund() {
        return vatToRefund;
    }

    public void setVatToRefund(BigDecimal vatToRefund) {
        this.vatToRefund = vatToRefund;
    }

    public BigDecimal getEffectiveVatToPay() {
        return effectiveVatToPay;
    }

    public void setEffectiveVatToPay(BigDecimal effectiveVatToPay) {
        this.effectiveVatToPay = effectiveVatToPay;
    }

    public BigDecimal getVatForDeduction() {
        return vatForDeduction;
    }

    public void setVatForDeduction(BigDecimal vatForDeduction) {
        this.vatForDeduction = vatForDeduction;
    }

    public BigDecimal getVatRefundArt92() {
        return vatRefundArt92;
    }

    public void setVatRefundArt92(BigDecimal vatRefundArt92) {
        this.vatRefundArt92 = vatRefundArt92;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
