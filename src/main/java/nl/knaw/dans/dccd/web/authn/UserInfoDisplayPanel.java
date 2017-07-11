/*******************************************************************************
 * Copyright 2015 DANS - Data Archiving and Networked Services
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package nl.knaw.dans.dccd.web.authn;
/*
 * Note by pboon: was in easy package nl.knaw.dans.easy.web.authn;
 */

import nl.knaw.dans.common.lang.user.User;
import nl.knaw.dans.common.web.template.AbstractCommonStatelessForm;
import nl.knaw.dans.common.web.template.AbstractCommonStatelessPanel;
import nl.knaw.dans.dccd.application.services.DccdUserService;
import nl.knaw.dans.dccd.application.services.UserServiceException;
import nl.knaw.dans.dccd.common.web.SwitchPanel;
import nl.knaw.dans.dccd.model.DccdOrganisation;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.model.DccdOrganisation.State;
import nl.knaw.dans.dccd.web.DccdSession;
import nl.knaw.dans.dccd.web.authn.UserStatusChangeActionSelection.Action;
import nl.knaw.dans.dccd.web.error.ErrorPage;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserInfoDisplayPanel extends AbstractCommonStatelessPanel
{
	private static Logger logger = LoggerFactory.getLogger(UserInfoDisplayPanel.class);

    private static final String WR_EDIT_LINK = "editLink";
    private static final String WR_CHANGE_PASSWORD_LINK = "changePasswordLink";
    private static final long serialVersionUID = 2646103426056079L;
    private final SwitchPanel parent;
    private final boolean enableModeSwitch;

	private DccdUser userLogedIn = (DccdUser)((DccdSession) getSession()).getUser();
	private boolean admin = (userLogedIn != null && userLogedIn.hasRole(DccdUser.Role.ADMIN));
	

	UserStatusChangeForm statusChangeForm;

    public UserInfoDisplayPanel(final SwitchPanel parent, final String userId, final boolean enableModeSwitch)
    {
        super(SwitchPanel.SWITCH_PANEL_WI);
        this.parent = parent;
        this.enableModeSwitch = enableModeSwitch;
        init(userId);
    }

    private void init(final String userId)
    {
        User user = null;
        try
        {
            user = DccdUserService.getService().getUserById(userId);
        }
        catch (UserServiceException e)
        {
            error("User with userId '" + userId + "' not found.");
        }

        if (user == null)
        {
            throw new RestartResponseException(new ErrorPage());
        }
        else
        {
            constructPanel(user);
        }
    }

    @Override
	protected void onBeforeRender()
    {
    	// we need to update the status form,
    	// because it uses a panel that is not updated when the users status changes
    	UserStatusChangeForm newStatusChangeForm = new UserStatusChangeForm("userInfoForm", (DccdUser)getDefaultModelObject());
    	newStatusChangeForm.setVisible(admin);
    	statusChangeForm.replaceWith(newStatusChangeForm);
    	statusChangeForm = newStatusChangeForm;

		super.onBeforeRender();
	}

    private void constructPanel(final User user)
    {
        super.setDefaultModel(new CompoundPropertyModel(user));

        DccdUser sessionUser = (DccdUser)((DccdSession)Session.get()).getUser();
        // TODO: there might be no session user, when we get here via a
        // link from outside the webapp
        boolean hasAdminRole = sessionUser.hasRole(DccdUser.Role.ADMIN);
        boolean isSessionUser = user.getId().equals(sessionUser.getId());

		// The actions that change the user's status
		statusChangeForm = new UserStatusChangeForm("userInfoForm", (DccdUser)user);
		statusChangeForm.setVisible(hasAdminRole);
		add(statusChangeForm);

        add(new Label(UserProperties.USER_ID));
        add(new Label(UserProperties.DISPLAYNAME));
        add(new Label(UserProperties.EMAIL));
        add(new Label(UserProperties.TITLE));
        add(new Label(UserProperties.INITIALS));
        add(new Label(UserProperties.PREFIXES));
        add(new Label(UserProperties.SURNAME));
        add(new Label(UserProperties.FUNCTION));
        add(new Label(UserProperties.TELEPHONE));
        add(new Label(UserProperties.DAI));

        // Edit
        Link modeSwitch = new Link(WR_EDIT_LINK)
        {
            private static final long serialVersionUID = -804946462543838511L;

            @Override
            public void onClick()
            {
                parent.switchMode();
            }
        };
        modeSwitch.add(new Label(WR_EDIT_LINK, new ResourceModel(WR_EDIT_LINK)));
        modeSwitch.setVisible(enableModeSwitch);
        add(modeSwitch);

        // ChangePassword
        Link changePasswordLink = new Link(WR_CHANGE_PASSWORD_LINK)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick()
            {
                setResponsePage(ChangePasswordPage.class);
            }
        };
        changePasswordLink.setVisible(isSessionUser);
        changePasswordLink.add(new Label(WR_CHANGE_PASSWORD_LINK, new ResourceModel(WR_CHANGE_PASSWORD_LINK)));
        add(changePasswordLink);

        // Organistion
        Link organisationLink = new Link("organisationLink", new ResourceModel("organisationLink"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick()
            {
            	((DccdSession)Session.get()).setRedirectPage(OrganisationPage.class, getPage());

            	String organisationId = user.getOrganization();
            	boolean organisationEdit = false; // never start in edit mode
            	boolean allowOrganisationEdit = enableModeSwitch && admin;
                setResponsePage(new OrganisationPage(organisationId, organisationEdit, allowOrganisationEdit));
            }
        };
        // assume the organisation field of the user is the organisation Id!
        organisationLink.add(new Label("organization", user.getOrganization()));
        add(organisationLink);
    }

    class UserStatusChangeForm extends AbstractCommonStatelessForm
    {
		private static final long serialVersionUID = 2555750334006800984L;
		private UserStatusChangeActionSelection actionSelection;

		public UserStatusChangeForm(String wicketId, DccdUser user)
		{
			super(wicketId, new CompoundPropertyModel(user), false);

	        Label stateLabel = new Label("state",
	                new StringResourceModel("user.states.${state}", this, getModel()));
	        add(stateLabel);

			// based on the user status add allowed actions
			switch (user.getState())
			{
				case REGISTERED:
					actionSelection = new UserStatusChangeActionSelection(Action.ACTIVATE, Action.DELETE);
					// add a confirm for activation, only if needed!
					try
					{
						boolean needsConfirmation = checkIfOrganisationNeedsActivation(user);
						logger.debug("Confirmation needed: " + needsConfirmation);
						if (needsConfirmation)
						{
							// TODO construct nice message from properties file
							actionSelection.addConfirmation(Action.ACTIVATE, "This will also Activate the organisation. \\nContinue?");
						}
					}
					catch (UserServiceException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case ACTIVE:
					actionSelection = new UserStatusChangeActionSelection(Action.DELETE);
					break;
				case BLOCKED:
					actionSelection = new UserStatusChangeActionSelection(Action.RESTORE);
					break;
				default:
					// allow nothing
					actionSelection = new UserStatusChangeActionSelection();
			}

			// add components here
			add(new UserStatusChangeActionSelectionPanel("statePanel", actionSelection));
		}

		@Override
		protected void onSubmit() {
			// get the selected action
			logger.debug("Selected change action:" + actionSelection.getSelectedAction());

			((DccdSession)Session.get()).setRedirectPage(ChangeMembershipPage.class, getPage());
			DccdUser user = (DccdUser)getModelObject();
			switch(actionSelection.getSelectedAction())
			{
				case ACTIVATE:
					setResponsePage(new ChangeMembershipPage(user, ChangeMembershipPage.MessageType.ACTIVATION));
					break;
				case DELETE:
					setResponsePage(new ChangeMembershipPage(user, ChangeMembershipPage.MessageType.DELETION));
					break;
				case RESTORE:
					// Make user REGISTERED instead of ACTIVE!
					user.setState(User.State.REGISTERED);
				    try
				    {
				    	DccdUserService.getService().update(userLogedIn, user);
					    logger.debug("Member restored to registered state");
					}
				    catch (UserServiceException e)
				    {
					    logger.error("Could not restore member", e);
					}

					// Note: was activate instead of registered
					//setResponsePage(new ContactMemberAboutMembershipPage(user, ContactMemberAboutMembershipPage.MessageType.ACTIVATION));
					break;
			}
		}
    }

	/**
	 *  When the user is being activated,
	 *  check if organisation also needs to be activated
	 */
	private boolean checkIfOrganisationNeedsActivation(DccdUser user) throws UserServiceException
	{
		boolean needsActivation = false;
		// get the user's organisation
		String organisationId = user.getOrganization();
		DccdOrganisation  organisation = DccdUserService.getService().getOrganisationById(organisationId);
		if (!organisation.getState().equals(State.ACTIVE))
		{
			// organistation needs to be activated
			needsActivation = true;
		}

		return needsActivation;
	}

}
