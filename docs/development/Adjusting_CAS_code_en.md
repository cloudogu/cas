# Adaptation of the CAS code

There are situations where we want to modify the behavior of the CAS and for this purpose the code of the CAS has to be
adapted or be overwritten. While much of the CAS behavior can be controlled via properties, not all of it can. In this
article will describe with practical examples, how the behavior of the CAS can be modified by adapting the source code.
source code can be modified.

CAS itself is open source. The source code of CAS can be found on Github: https://github.com/apereo/cas/ . The
modifications are only made in our CAS repository - no changes are made to the original CAS code. In addition,
"overwriting" means here, the extension of Java classes, in which individual methods are then methods are overwritten,
as well as the adaptation of texts by using certain properties.

The places to be adapted and to be overwritten must be found independently in the CAS code. For the search can e.g.
error messages, display texts or log outputs can be helpful for the search.

## Example 1: No error message when resetting password if username does not exist

### Use case

The 1st example is about customizing an error message for the password reset function. This functionality works in such
a way that a user has to enter his username and then gets an e-mail with a link to change his password.

The behavior of the CAS is that an error message is displayed if the username entered does not exist in the system. This
is helpful for the user - e.g. if he has mistyped his password - but at the same time it bears the risk that existing
and non-existing usernames can be determined in the system.

The behavior has been adjusted so that no error message appears if the username does not exist in the system. The
notification text that an e-mail has been sent has also been extended to include a note. In This note states that the
e-mail has only been sent if the user actually exists in the system.

### Adjusted source code

The changes have been made as part of Issue #161: https://github.com/cloudogu/cas/issues/161 .

#### Adding required dependencies

In order to be able to override the CAS code, it is necessary for the compilation to include all the required
dependencies are included. Dependencies are included in the `build.gradle` data. For this example the CAS
dependencies `cas-server-core-notification`, `cas-server-core-notification`, `cas-server-core-notification`
, `cas-server-core-notification`, `cas-server-support-pm-core` and `cas-server-support-pm-webflow` had to be included.

These dependencies had to be included because classes from these dependencies are used in the overridden source code.

#### Customization Backend Code

At the core of this feature are the classes `CesSendPasswordResetInstructionsAction` and `PmConfiguration`.

The class `CesSendPasswordInstructions` extends the original class from CAS and overwrites the corresponding
method - in this case the method `doExecute`. Practically the original CAS code can be copied here first and then adapt
it accordingly. In order to avoid duplicated code, if possible, not the entire method should be copied, but the super
method should be used and called.

In the class `PmConfiguration` the bean is created. Also here the code is copied for the most part. Only here
instead of an instance of `SendPasswordInstructions` an instance of our own class `CesSendPasswordInstructions` is
created.

So that the creation of the bean in the class `PmConfiguration` is also attracted, this class must be created in the
file `spring.factories` and `resources/META-INF`.

For classes that extend a CAS class, it is a good naming convention to put `Ces` at the beginning of the class name.
at the beginning of the class name. This way the extended and customized classes can be identified quickly.

#### Possibility for unit tests

To test the customized code, the beans can be mocked.

It becomes a bit more difficult if the method to be tested, calls a more complex super method. For this case there is
a workaround. The class, in which the method to be tested is, is extended for the overwritten test and the called super
method is overwritten. The overridden method simply returns a certain value.

An example of this can be found in the class `CesSendPasswordResetInstructionsActionTest`.

#### Customization frontend code

The texts at the interface of the CAS are stored for the respective language as properties in a `.properties` file.

To be able to customize and extend the message text of a sent e-mail, the corresponding text properties must be adapted
to German and English.

The corresponding property key for the hint test is `screen.pm.reset.sentInstructions`. To override the text for this
you have to edit the properties files `custom_messages.properties` and `custom_messages_en.properties` in the `app`
-directory under `src/main/resources` and store the corresponding text as value.

For the German texts, make sure that umlauts are specified in encoded form, e.g. `\u00FC` for an `ü`.

After adjusting the text - the text has now become longer - check whether it is still displayed sensibly in the
interface. This is the case in this example. If this is not the case, changes would still have to be made to the
corresponding Thymeleaf template or the HTML fragment.

#### Data-TestIDs for integration tests

When creating integration tests, it is useful to store a unique Data-TestID in the HTML elements. Using this ID, the
integration test can then uniquely access an element and no adjustments need to be made to the integration test.
This way, no adjustments have to be made to the integration tests if something else changes in the HTML element.