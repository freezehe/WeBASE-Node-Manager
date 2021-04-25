package com.webank.webase.node.mgr.contract.warehouse.mapper;

import com.webank.webase.node.mgr.contract.warehouse.entity.TbContractFolder;
import org.apache.ibatis.jdbc.SQL;

public class TbContractFolderSqlProvider {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database table tb_contract_folder
     *
     * @mbg.generated
     */
    public static final String ALL_COLUMN_FIELDS = "id,folder_name,create_time,modify_time,ware_house_id,description,description_en,folder_detail,folder_detail_en";

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_contract_folder
     *
     * @mbg.generated
     */
    public String insertSelective(TbContractFolder record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("tb_contract_folder");
        
        if (record.getFolderName() != null) {
            sql.VALUES("folder_name", "#{folderName,jdbcType=VARCHAR}");
        }
        
        if (record.getCreateTime() != null) {
            sql.VALUES("create_time", "#{createTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getModifyTime() != null) {
            sql.VALUES("modify_time", "#{modifyTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getWareHouseId() != null) {
            sql.VALUES("ware_house_id", "#{wareHouseId,jdbcType=INTEGER}");
        }
        
        if (record.getDescription() != null) {
            sql.VALUES("description", "#{description,jdbcType=LONGVARCHAR}");
        }
        
        if (record.getDescriptionEn() != null) {
            sql.VALUES("description_en", "#{descriptionEn,jdbcType=LONGVARCHAR}");
        }
        
        if (record.getFolderDetail() != null) {
            sql.VALUES("folder_detail", "#{folderDetail,jdbcType=LONGVARCHAR}");
        }
        
        if (record.getFolderDetailEn() != null) {
            sql.VALUES("folder_detail_en", "#{folderDetailEn,jdbcType=LONGVARCHAR}");
        }
        
        return sql.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_contract_folder
     *
     * @mbg.generated
     */
    public String updateByPrimaryKeySelective(TbContractFolder record) {
        SQL sql = new SQL();
        sql.UPDATE("tb_contract_folder");
        
        if (record.getFolderName() != null) {
            sql.SET("folder_name = #{folderName,jdbcType=VARCHAR}");
        }
        
        if (record.getCreateTime() != null) {
            sql.SET("create_time = #{createTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getModifyTime() != null) {
            sql.SET("modify_time = #{modifyTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getWareHouseId() != null) {
            sql.SET("ware_house_id = #{wareHouseId,jdbcType=INTEGER}");
        }
        
        if (record.getDescription() != null) {
            sql.SET("description = #{description,jdbcType=LONGVARCHAR}");
        }
        
        if (record.getDescriptionEn() != null) {
            sql.SET("description_en = #{descriptionEn,jdbcType=LONGVARCHAR}");
        }
        
        if (record.getFolderDetail() != null) {
            sql.SET("folder_detail = #{folderDetail,jdbcType=LONGVARCHAR}");
        }
        
        if (record.getFolderDetailEn() != null) {
            sql.SET("folder_detail_en = #{folderDetailEn,jdbcType=LONGVARCHAR}");
        }
        
        sql.WHERE("id = #{id,jdbcType=INTEGER}");
        
        return sql.toString();
    }
}