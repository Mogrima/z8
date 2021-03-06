Z8.define('Z8.calendar.Dropdown', {
	extend: 'Z8.calendar.Calendar',

	cls: 'dropdown-calendar display-none',
	visible: false,
	autoAlign: true,

	show: function(top, left) {
		if(this.isVisible()) {
			DOM.focus(this.selectedDay);
			return;
		}

		this.setPosition(top, left);

		this.callParent();
		this.align();

		this.fireEvent('show', this);

		DOM.focus(this.selectedDay);
	},

	hide: function() {
		if(!this.isVisible())
			return;

		this.callParent();

		this.fireEvent('hide', this);
	},

	toggle: function() {
		this.visible ? this.hide() : this.show();
	},

	onDayClick: function(day) {
		this.hide();
		this.callParent(day);
	},

	onCancel: function() {
		this.hide();
		this.fireEvent('cancel', this);
	},

	onKeyDown: function(event, target) {
		if(this.callParent(event, target))
			return;

		var key = event.getKey();
	
		if(key == Event.ESC)
			this.onCancel();
		else if(key == Event.TAB) {
			if(!event.shiftKey && target == this.minute)
				DOM.focus(this.previousMonth);
			else if(event.shiftKey && target == this.previousMonth)
				DOM.focus(this.minute);
			else
				return false;
		} else
			return false;

		event.stopEvent();
		return true;
	}
});