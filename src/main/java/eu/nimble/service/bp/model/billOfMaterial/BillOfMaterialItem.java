package eu.nimble.service.bp.model.billOfMaterial;

import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

public class BillOfMaterialItem {

    private String catalogueUuid;
    private String lineId;
    private QuantityType quantity;

    /**
     * Get catalogueUuid
     *
     * @return catalogueUuid
     **/
    @ApiModelProperty(value = "")
    public String getCatalogueUuid() {
        return catalogueUuid;
    }

    public void setcatalogueUuid(String catalogueUuid) {
        this.catalogueUuid = catalogueUuid;
    }

    /**
     * Get lineId
     *
     * @return lineId
     **/
    @ApiModelProperty(value = "")
    public String getlineId() {
        return lineId;
    }

    public void setlineId(String lineId) {
        this.lineId = lineId;
    }

    /**
     * Get quantity
     *
     * @return quantity
     **/
    @ApiModelProperty(value = "")
    public QuantityType getquantity() {
        return quantity;
    }

    public void setquantity(QuantityType quantity) {
        this.quantity = quantity;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BillOfMaterialItem billOfMaterialItem = (BillOfMaterialItem) o;
        return Objects.equals(this.catalogueUuid, billOfMaterialItem.catalogueUuid) &&
                Objects.equals(this.lineId, billOfMaterialItem.lineId) &&
                Objects.equals(this.quantity, billOfMaterialItem.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(catalogueUuid,lineId,quantity);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BillOfMaterialItem {\n");

        sb.append("    catalogueUuid: ").append(toIndentedString(catalogueUuid)).append("\n");
        sb.append("    lineId: ").append(toIndentedString(lineId)).append("\n");
        sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
