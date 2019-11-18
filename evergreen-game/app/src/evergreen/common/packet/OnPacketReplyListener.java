package evergreen.common.packet;

/**
 * Created by Emil on 2017-03-25.
 */

public interface OnPacketReplyListener {
    public void onSuccess();
    public void onFailure();
}