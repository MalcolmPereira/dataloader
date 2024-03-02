package com.malcolm.dataloader;

/**
 * Product Token
 * <p/>
 * Product Token is a unique identifier for a product. It is a combination of product code, region code and product serial contains additional product meta-data
 * <p/>
 * @author Malcolm
 */
public class ProductToken {
    /**
     * Product Code
     */
    private String productCode;

    /**
     * Region Code
     */
    private String regionCode;

    /**
     * Product Serial
     */
    private Integer productSerial;

    /**
     * Product Token Name
     */
    private String productTokenName;

    /**
     * Product Token Values
     */
    private String productTokenValues;

    /**
     * Product Token Values Hash
     */
    private Long productTokenValuesHash;

   /**
     * Get Product Code
     * @return Product Code
     */
    public String getProductCode() {
        return productCode;
    }

    /**
     * Set Product Code
     * @param productCode Product Code
     */
    public void setProductCode(final String productCode) {
        this.productCode = productCode;
    }

    /**
     * Get Region Code
     * @return Region Code
     */
    public String getRegionCode() {
        return regionCode;
    }

    /**
     * Set Region Code
     * @param regionCode Region Code
     */
    public void setRegionCode(final String regionCode) {
        this.regionCode = regionCode;
    }

    /**
     * Get Product Serial
     * @return Product Serial
     */
    public Integer getProductSerial() {
        return productSerial;
    }

    /**
     * Set Product Serial
     * @param productSerial Product Serial
     */
    public void setProductSerial(final Integer productSerial) {
        this.productSerial = productSerial;
    }

    /**
     * Get Product Token Name
     * @return Product Token Name
     */
    public String getProductTokenName() {
        return productTokenName;
    }

    /**
     * Set Product Token Name
     * @param productTokenName Product Token Name
     */
    public void setProductTokenName(final String productTokenName) {
        this.productTokenName = productTokenName;
    }

    /**
     * Get Product Token Values
     * @return Product Token Values
     */
    public String getProductTokenValues() {
        return productTokenValues;
    }

    /**
     * Set Product Token Values
     * @param productTokenValues Product Token Values
     */
    public void setProductTokenValues(final String productTokenValues) {
        this.productTokenValues = productTokenValues;
    }

    /**
     * Get Product Token Values Hash
     * @return Product Token Values Hash
     */
    public Long getProductTokenValuesHash() {
        return productTokenValuesHash;
    }

    /**
     * Set Product Token Values Hash
     * @param productTokenValuesHash Product Token Values Hash
     */
    public void setProductTokenValuesHash(final Long productTokenValuesHash) {
        this.productTokenValuesHash = productTokenValuesHash;
    }

    /**
     * Check if Product Token is valid
     * @return true if Product Token is valid
     */
    public boolean isValidProductToken() {
        if (productCode == null || regionCode == null || productSerial == null || productTokenName == null || productTokenValues == null) {
            return false;
        }
        return !productCode.isEmpty() && !regionCode.isEmpty() && !productTokenName.isEmpty() && !productTokenValues.isEmpty() && !productCode.isBlank() && !regionCode.isBlank() && !productTokenName.isBlank() && !productTokenValues.isBlank();
    }

    /**
     * Equals Method
     * @param o object to compare
     * @return boolean The Boolean value to return
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ProductToken that = (ProductToken) o;

        if (!regionCode.equals(that.regionCode)) return false;
        return productTokenValues.equals(that.productTokenValues);
    }

    /**
     * Hash Code
     * @return int Hash Code
     */
    @Override
    public int hashCode() {
        int result = regionCode.hashCode();
        result = 31 * result + productTokenValues.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ProductToken{" +
                ", productCode='" + productCode + '\'' +
                ", regionCode='" + regionCode + '\'' +
                ", productSerial=" + productSerial +
                ", productTokenName='" + productTokenName + '\'' +
                ", productTokenValues='" + ((productTokenValues != null ) ?productTokenValues.length() : "") + '\'' +
                '}';
    }
}
