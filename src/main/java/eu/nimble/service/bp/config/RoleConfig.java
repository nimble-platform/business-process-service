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
                    NimbleRole.SALES_OFFICER};
    public static final NimbleRole[] REQUIRED_ROLES_PURCHASES =
            {NimbleRole.COMPANY_ADMIN,
                    NimbleRole.EXTERNAL_REPRESENTATIVE,
                    NimbleRole.LEGAL_REPRESENTATIVE,
                    NimbleRole.PURCHASER};
    public static final NimbleRole[] REQUIRED_ROLES_PURCHASES_OR_SALES =
            {NimbleRole.COMPANY_ADMIN,
                    NimbleRole.EXTERNAL_REPRESENTATIVE,
                    NimbleRole.LEGAL_REPRESENTATIVE,
                    NimbleRole.PURCHASER,
                    NimbleRole.SALES_OFFICER};
    public static final NimbleRole[] REQUIRED_ROLES_ADMIN =
            {NimbleRole.COMPANY_ADMIN,
                    NimbleRole.EXTERNAL_REPRESENTATIVE,
                    NimbleRole.LEGAL_REPRESENTATIVE};
}
