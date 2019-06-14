//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.05.29 at 02:01:18 PM MSK
//


package eu.nimble.service.bp.hyperjaxb.model;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.JAXBEqualsStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;


/**
 * <p>Java class for ProcessInstanceDAO complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ProcessInstanceDAO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="processInstanceID" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="creationDate" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="processID" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="status" type="{}ProcessInstanceStatus"/&gt;
 *         &lt;element name="precedingProcess" type="{}ProcessInstanceDAO"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProcessInstanceDAO", propOrder = {
    "processInstanceID",
    "creationDate",
    "processID",
    "status",
    "precedingProcess"
})
@Entity(name = "ProcessInstanceDAO")
@Table(name = "PROCESS_INSTANCE_DAO")
@Inheritance(strategy = InheritanceType.JOINED)
public class ProcessInstanceDAO
    implements Equals
{

    @XmlElement(required = true)
    protected String processInstanceID;
    @XmlElement(required = true)
    protected String creationDate;
    @XmlElement(required = true)
    protected String processID;
    @XmlElement(required = true)
    @XmlSchemaType(name = "token")
    protected ProcessInstanceStatus status;
    @XmlElement(required = true)
    protected ProcessInstanceDAO precedingProcess;
    @XmlAttribute(name = "Hjid")
    protected Long hjid;

    /**
     * Gets the value of the processInstanceID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Basic
    @Column(name = "PROCESS_INSTANCE_ID", length = 255)
    public String getProcessInstanceID() {
        return processInstanceID;
    }

    /**
     * Sets the value of the processInstanceID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProcessInstanceID(String value) {
        this.processInstanceID = value;
    }

    /**
     * Gets the value of the creationDate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Basic
    @Column(name = "CREATION_DATE", length = 255)
    public String getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the value of the creationDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreationDate(String value) {
        this.creationDate = value;
    }

    /**
     * Gets the value of the processID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Basic
    @Column(name = "PROCESS_ID", length = 255)
    public String getProcessID() {
        return processID;
    }

    /**
     * Sets the value of the processID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProcessID(String value) {
        this.processID = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link ProcessInstanceStatus }
     *     
     */
    @Basic
    @Column(name = "STATUS", length = 255)
    @Enumerated(EnumType.STRING)
    public ProcessInstanceStatus getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProcessInstanceStatus }
     *     
     */
    public void setStatus(ProcessInstanceStatus value) {
        this.status = value;
    }

    /**
     * Gets the value of the precedingProcess property.
     * 
     * @return
     *     possible object is
     *     {@link ProcessInstanceDAO }
     *     
     */
    @OneToOne(targetEntity = ProcessInstanceDAO.class, cascade = {
        CascadeType.ALL,
        CascadeType.REFRESH,
        CascadeType.PERSIST,
        CascadeType.MERGE
    })
    @JoinColumn(name = "PRECEDING_PROCESS_PROCESS_IN_0")
    public ProcessInstanceDAO getPrecedingProcess() {
        return precedingProcess;
    }

    /**
     * Sets the value of the precedingProcess property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProcessInstanceDAO }
     *     
     */
    public void setPrecedingProcess(ProcessInstanceDAO value) {
        this.precedingProcess = value;
    }

    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if ((object == null)||(this.getClass()!= object.getClass())) {
            return false;
        }
        if (this == object) {
            return true;
        }
        final ProcessInstanceDAO that = ((ProcessInstanceDAO) object);
        {
            String lhsProcessInstanceID;
            lhsProcessInstanceID = this.getProcessInstanceID();
            String rhsProcessInstanceID;
            rhsProcessInstanceID = that.getProcessInstanceID();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "processInstanceID", lhsProcessInstanceID), LocatorUtils.property(thatLocator, "processInstanceID", rhsProcessInstanceID), lhsProcessInstanceID, rhsProcessInstanceID)) {
                return false;
            }
        }
        {
            String lhsCreationDate;
            lhsCreationDate = this.getCreationDate();
            String rhsCreationDate;
            rhsCreationDate = that.getCreationDate();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "creationDate", lhsCreationDate), LocatorUtils.property(thatLocator, "creationDate", rhsCreationDate), lhsCreationDate, rhsCreationDate)) {
                return false;
            }
        }
        {
            String lhsProcessID;
            lhsProcessID = this.getProcessID();
            String rhsProcessID;
            rhsProcessID = that.getProcessID();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "processID", lhsProcessID), LocatorUtils.property(thatLocator, "processID", rhsProcessID), lhsProcessID, rhsProcessID)) {
                return false;
            }
        }
        {
            ProcessInstanceStatus lhsStatus;
            lhsStatus = this.getStatus();
            ProcessInstanceStatus rhsStatus;
            rhsStatus = that.getStatus();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "status", lhsStatus), LocatorUtils.property(thatLocator, "status", rhsStatus), lhsStatus, rhsStatus)) {
                return false;
            }
        }
        {
            ProcessInstanceDAO lhsPrecedingProcess;
            lhsPrecedingProcess = this.getPrecedingProcess();
            ProcessInstanceDAO rhsPrecedingProcess;
            rhsPrecedingProcess = that.getPrecedingProcess();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "precedingProcess", lhsPrecedingProcess), LocatorUtils.property(thatLocator, "precedingProcess", rhsPrecedingProcess), lhsPrecedingProcess, rhsPrecedingProcess)) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object object) {
        final EqualsStrategy strategy = JAXBEqualsStrategy.INSTANCE;
        return equals(null, null, object, strategy);
    }

    /**
     * Gets the value of the hjid property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    @Id
    @Column(name = "HJID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getHjid() {
        return hjid;
    }

    /**
     * Sets the value of the hjid property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setHjid(Long value) {
        this.hjid = value;
    }

}
