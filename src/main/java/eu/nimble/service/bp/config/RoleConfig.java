package eu.nimble.service.bp.config;

import eu.nimble.utility.validation.NimbleRole;

/**
 * Created by suat on 09-Sep-19.
 */
public class RoleConfig {
    public static final NimbleRole[] REQUIRED_ROLES_SALES =
            {NimbleRole.COMPANY_ADMIN,
                    NimbleRole.EXTERNAL_REPRESENTATIVE,
                    NimbleRole.LEGAL_REPRESENTATIVE,
                    NimbleRole.INITIAL_REPRESENTATIVE,
                    NimbleRole.SALES_OFFICER,
                    NimbleRole.EFACTORY_USER};
    public static final NimbleRole[] REQUIRED_ROLES_PURCHASES =
            {NimbleRole.COMPANY_ADMIN,
                    NimbleRole.EXTERNAL_REPRESENTATIVE,
                    NimbleRole.LEGAL_REPRESENTATIVE,
                    NimbleRole.INITIAL_REPRESENTATIVE,
                    NimbleRole.PURCHASER,
                    NimbleRole.EFACTORY_USER};
    public static final NimbleRole[] REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE =
            {NimbleRole.COMPANY_ADMIN,
                    NimbleRole.EXTERNAL_REPRESENTATIVE,
                    NimbleRole.LEGAL_REPRESENTATIVE,
                    NimbleRole.INITIAL_REPRESENTATIVE,
                    NimbleRole.PURCHASER,
                    NimbleRole.SALES_OFFICER,
                    NimbleRole.EFACTORY_USER};
    public static final NimbleRole[] REQUIRED_ROLES_PURCHASES_OR_SALES_READ =
            {NimbleRole.COMPANY_ADMIN,
                    NimbleRole.EXTERNAL_REPRESENTATIVE,
                    NimbleRole.LEGAL_REPRESENTATIVE,
                    NimbleRole.INITIAL_REPRESENTATIVE,
                    NimbleRole.PURCHASER,
                    NimbleRole.SALES_OFFICER,
                    NimbleRole.MONITOR,
                    NimbleRole.EFACTORY_USER};
    public static final NimbleRole[] REQUIRED_ROLES_ADMIN =
            {NimbleRole.COMPANY_ADMIN,
                    NimbleRole.EXTERNAL_REPRESENTATIVE,
                    NimbleRole.INITIAL_REPRESENTATIVE,
                    NimbleRole.LEGAL_REPRESENTATIVE,
                    NimbleRole.EFACTORY_USER};
    public static final NimbleRole[] REQUIRED_ROLES_TO_EXPORT_PROCESS_INSTANCE_DATA =
            {NimbleRole.COMPANY_ADMIN,
                    NimbleRole.EXTERNAL_REPRESENTATIVE,
                    NimbleRole.LEGAL_REPRESENTATIVE,
                    NimbleRole.INITIAL_REPRESENTATIVE,
                    NimbleRole.NIMBLE_DELETED_USER,
                    NimbleRole.EFACTORY_USER};
    public static final NimbleRole[] REQUIRED_ROLES_TO_LOG_PAYMENTS =
            {NimbleRole.EFACTORY_USER};
}
