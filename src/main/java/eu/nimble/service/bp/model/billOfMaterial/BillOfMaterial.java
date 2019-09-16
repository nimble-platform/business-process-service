package eu.nimble.service.bp.model.billOfMaterial;

import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BillOfMaterial {

    private List<BillOfMaterialItem> billOfMaterialItems = new ArrayList<>();

    /**
     * Get billOfMaterialItems
     *
     * @return billOfMaterialItems
     **/
    @ApiModelProperty(value = "")
    public List<BillOfMaterialItem> getBillOfMaterialItems() {
        return billOfMaterialItems;
    }

    public void setBillOfMaterialItems(List<BillOfMaterialItem> billOfMaterialItems) {
        this.billOfMaterialItems = billOfMaterialItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BillOfMaterial billOfMaterial = (BillOfMaterial) o;
        return Objects.equals(this.billOfMaterialItems, billOfMaterial.billOfMaterialItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billOfMaterialItems);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BillOfMaterial {\n");

        sb.append("    billOfMaterialItems: ").append(toIndentedString(billOfMaterialItems)).append("\n");
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
