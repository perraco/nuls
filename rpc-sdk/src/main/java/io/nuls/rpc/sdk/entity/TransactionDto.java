package io.nuls.rpc.sdk.entity;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Vive
 */
public class TransactionDto {

    private String hash;

    private Integer type;

    private Integer index;

    private Long time;

    private Long blockHeight;

    private Double fee;

    private Double value;

    private List<InputDto> inputs;

    private List<OutputDto> outputs;

    /**
     * -1 transfer,  1 receiver
     */
    private Integer transferType;

    private String remark;

    private String sign;

    public TransactionDto(Map<String, Object> map){
        hash = (String) map.get("hash");
        type = (Integer) map.get("type");
        index = (Integer) map.get("index");
        time = (Long) map.get("time");
        blockHeight = (Long) map.get("blockHeight");
        fee = (Double) map.get("fee");
        value = (Double) map.get("value");
        transferType = (Integer) map.get("transferType");
        remark = (String) map.get("remark");
        sign = (String) map.get("sign");
        inputs = new ArrayList<>();
        List<Map<String,Object>> inputMapList = (List<Map<String, Object>>) map.get("inputs");
        for(Map<String, Object> inputMap:inputMapList){
            inputs.add(new InputDto(inputMap));
        }
        outputs = new ArrayList<>();
        List<Map<String,Object>> outputMapList = (List<Map<String, Object>>) map.get("outputs");
        for(Map<String, Object> outputMap:outputMapList){
            outputs.add(new OutputDto(outputMap));
        }
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public Double getFee() {
        return fee;
    }

    public void setFee(Double fee) {
        this.fee = fee;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public List<InputDto> getInputs() {
        return inputs;
    }

    public void setInputs(List<InputDto> inputs) {
        this.inputs = inputs;
    }

    public List<OutputDto> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<OutputDto> outputs) {
        this.outputs = outputs;
    }

    public Integer getTransferType() {
        return transferType;
    }

    public void setTransferType(Integer transferType) {
        this.transferType = transferType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

}
