package com.assistant.acc.domain.project;

import lombok.Data;

import java.util.Date;

/**
 * 현재 도메인에서 축제 정보를 객관적인 정보만 취급할지
 * 기획 의도와 같은 추상적 데이터도 취급 할 것인지에 따라서 도메인 분리 판단.
 */
@Data
public class Project {

    private Integer projectNo;
    private String memberNo;
    private Date createAt;

//    //getter & setter 정리
//    public Integer getPNo() {
//        return projectNo;
//    }
//    public void setPNo(Integer pNo) {
//        this.projectNo = pNo;
//    }
//    public String getMNo() {
//        return memberNo;
//    }
//    public void setMNo(String mNo) {
//        this.memberNo = mNo;
//    }
//    public Date getCreateAt() {
//        return createAt;
//    }
//    public void setCreateAt(Date createAt) {
//        this.createAt = createAt;
//    }
    
}
