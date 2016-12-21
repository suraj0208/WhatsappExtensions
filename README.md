# WhatsApp Extensions
## Synopsis

The project is an Xposed module for WhatsApp. The module includes set of features like Call over phone, lock individual chats, reminders, highlight groups etc.  

## Code Example

The module hooks methods of standard WhatsApp application.

To add the menu items onOptionMenuCreated is hooked and menu items are added. The clicks are handled by hooking onOptionsItemSelected.
Groups are highlighted by hooking setTag of the View class directly.

## Installation

Xposed module link:

http://repo.xposed.info/module/com.suraj.waext


## Contribute / Discuss

clone the project:

git clone https://<span></span>github.com/suraj0208/WhatsappExtensions.git

join the discussion:

http://forum.xda-developers.com/xposed/modules/mod-whatsapp-extensions-add-extra-t3452784
