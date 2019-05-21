/**
 * Copyright (C) 2016-2019 Salvatore Virga - salvo.virga@tum.de, Marco Esposito - marco.esposito@tum.de
 * Technische Universit�t M�nchen Chair for Computer Aided Medical Procedures and Augmented Reality Fakult�t
 * f�r Informatik / I16, Boltzmannstra�e 3, 85748 Garching bei M�nchen, Germany http://campar.in.tum.de All
 * rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.tum.in.camp.kuka.ros;

import iiwa_msgs.CartesianPose;
import iiwa_msgs.RedundancyInformation;
import geometry_msgs.Point;
import geometry_msgs.PoseStamped;
import geometry_msgs.Quaternion;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.node.service.ServiceServer;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava.tf.Transform;
import org.ros.rosjava.tf.pubsub.TransformListener;
import org.ros.time.TimeProvider;

import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy;
import com.kuka.roboticsAPI.geometricModel.AbstractFrame;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.redundancy.IRedundancyCollection;

import de.tum.in.camp.kuka.ros.CommandTypes.CommandType;

/**
 * This class provides ROS subscribers for ROS messages defined in the iiwa_msgs ROS package. It allows to
 * received messages of that type from ROS topics named with the following convention : <robot
 * name>/command/<iiwa message type> (e.g. MyIIWA/command/JointPosition)
 */
public class iiwaSubscriber extends AbstractNodeMain {
  private ConnectedNode node = null;

  // Service for reconfiguring control mode
  @SuppressWarnings("unused")
  private ServiceServer<iiwa_msgs.ConfigureControlModeRequest, iiwa_msgs.ConfigureControlModeResponse> configureControlModeServer = null;
  private ServiceResponseBuilder<iiwa_msgs.ConfigureControlModeRequest, iiwa_msgs.ConfigureControlModeResponse> configureControlModeCallback = null;

  @SuppressWarnings("unused")
  private ServiceServer<iiwa_msgs.TimeToDestinationRequest, iiwa_msgs.TimeToDestinationResponse> timeToDestinationServer = null;
  private ServiceResponseBuilder<iiwa_msgs.TimeToDestinationRequest, iiwa_msgs.TimeToDestinationResponse> timeToDestinationCallback = null;

  @SuppressWarnings("unused")
  private ServiceServer<iiwa_msgs.SetSpeedOverrideRequest, iiwa_msgs.SetSpeedOverrideResponse> setSpeedOverrideServer = null;
  private ServiceResponseBuilder<iiwa_msgs.SetSpeedOverrideRequest, iiwa_msgs.SetSpeedOverrideResponse> setSpeedOverrideCallback = null;
  
  @SuppressWarnings("unused")
  private ServiceServer<iiwa_msgs.SetPTPCartesianSpeedLimitsRequest, iiwa_msgs.SetPTPCartesianSpeedLimitsResponse> setPTPCartesianSpeedLimitsServer = null;
  private ServiceResponseBuilder<iiwa_msgs.SetPTPCartesianSpeedLimitsRequest, iiwa_msgs.SetPTPCartesianSpeedLimitsResponse> setPTPCartesianSpeedLimitsCallback = null;

  @SuppressWarnings("unused")
  private ServiceServer<iiwa_msgs.SetPTPJointSpeedLimitsRequest, iiwa_msgs.SetPTPJointSpeedLimitsResponse> setPTPJointSpeedLimitsServer = null;
  private ServiceResponseBuilder<iiwa_msgs.SetPTPJointSpeedLimitsRequest, iiwa_msgs.SetPTPJointSpeedLimitsResponse> setPTPJointSpeedLimitsCallback = null;

  @SuppressWarnings("unused")
  private ServiceServer<iiwa_msgs.SetSmartServoJointSpeedLimitsRequest, iiwa_msgs.SetSmartServoJointSpeedLimitsResponse> setSmartServoJointSpeedLimitsServer = null;
  private ServiceResponseBuilder<iiwa_msgs.SetSmartServoJointSpeedLimitsRequest, iiwa_msgs.SetSmartServoJointSpeedLimitsResponse> setSmartServoJointSpeedLimitsCallback = null;

  @SuppressWarnings("unused")
  private ServiceServer<iiwa_msgs.SetSmartServoLinSpeedLimitsRequest, iiwa_msgs.SetSmartServoLinSpeedLimitsResponse> setSmartServoLinSpeedLimitsServer = null;
  private ServiceResponseBuilder<iiwa_msgs.SetSmartServoLinSpeedLimitsRequest, iiwa_msgs.SetSmartServoLinSpeedLimitsResponse> setSmartServoLinSpeedLimitsCallback = null;

  @SuppressWarnings("unused")
  private ServiceServer<iiwa_msgs.SetWorkpieceRequest, iiwa_msgs.SetWorkpieceResponse> setWorkpieceServer = null;
  private ServiceResponseBuilder<iiwa_msgs.SetWorkpieceRequest, iiwa_msgs.SetWorkpieceResponse> setWorkpieceCallback = null;

  @SuppressWarnings("unused")
  private ServiceServer<iiwa_msgs.SetEndpointFrameRequest, iiwa_msgs.SetEndpointFrameResponse> setEndpointFrameServer = null;
  private ServiceResponseBuilder<iiwa_msgs.SetEndpointFrameRequest, iiwa_msgs.SetEndpointFrameResponse> setEndpointFrameCallback = null;

  // ROSJava Subscribers for iiwa_msgs
  private Subscriber<geometry_msgs.PoseStamped> cartesianPoseSubscriber;
  private Subscriber<geometry_msgs.PoseStamped> cartesianPoseLinSubscriber;
  private Subscriber<geometry_msgs.TwistStamped> cartesianVelocitySubscriber;
  private Subscriber<iiwa_msgs.JointPosition> jointPositionSubscriber;
  private Subscriber<iiwa_msgs.JointPositionVelocity> jointPositionVelocitySubscriber;
  private Subscriber<iiwa_msgs.JointVelocity> jointVelocitySubscriber;

  private TransformListener tfListener;

  // Object to easily build iiwa_msgs from the current robot state
  private MessageGenerator helper;

  // Local iiwa_msgs to store received messages
  private geometry_msgs.PoseStamped cp;
  private geometry_msgs.PoseStamped cp_lin;
  private geometry_msgs.TwistStamped cv;
  private iiwa_msgs.JointPosition jp;
  private iiwa_msgs.JointPositionVelocity jpv;
  private iiwa_msgs.JointVelocity jv;

  private Boolean new_jp = new Boolean("false");
  private Boolean new_cp = new Boolean("false");
  private Boolean new_cp_lin = new Boolean("false");
  private Boolean new_jpv = new Boolean("false");

  // Current control strategy
  public CommandType currentCommandType = null;

  // Current action type
  public CommandType currentActionType = null;

  // Name to use to build the name of the ROS topics
  private String iiwaName = "iiwa";
  private LBR robot = null;
  private Boolean enforceMessageSequence = false;

  /**
   * Constructs a series of ROS subscribers for messages defined by the iiwa_msgs ROS package.
   * <p>
   * While no messages are received, the initial values are set to the state of the robot at the moment of
   * this call. For Cartesian messages, the initial values will refer to the frame of the robot's Flange.
   * 
   * @param robot: the current state of the robot is used to set up initial values for the messages.
   * @param robotName: name of the robot, it will be used for the topic names with this format : <robot
   *          name>/command/<iiwa message type>
   */
  public iiwaSubscriber(LBR robot, String robotName, TimeProvider timeProvider, Boolean enforceMessageSequence) {
    this(robot, robot.getFlange(), robotName, timeProvider, enforceMessageSequence);
  }

  /**
   * Constructs a series of ROS subscribers for messages defined by the iiwa_msgs ROS package.
   * <p>
   * While no messages are received, the initial values are set to the state of the robot at the moment of
   * this call.<br>
   * For Cartesian messages, the initial values will refer to the given frame.
   * 
   * @param robot: an iiwa Robot, its current state is used to set up initial values for the messages.
   * @param frame: reference frame to set the values of the Cartesian position.
   * @param robotName : name of the robot, it will be used for the topic names with this format : <robot
   *          name>/command/<iiwa message type>
   */
  public iiwaSubscriber(LBR robot, ObjectFrame frame, String robotName, TimeProvider timeProvider, Boolean enforceMessageSequence) {
    iiwaName = robotName;
    this.robot = robot;
    this.enforceMessageSequence = enforceMessageSequence;
    helper = new MessageGenerator(iiwaName, timeProvider);

    cp = helper.buildMessage(geometry_msgs.PoseStamped._TYPE);
    cp_lin = helper.buildMessage(geometry_msgs.PoseStamped._TYPE);
    cv = helper.buildMessage(geometry_msgs.TwistStamped._TYPE);
    jp = helper.buildMessage(iiwa_msgs.JointPosition._TYPE);
    jpv = helper.buildMessage(iiwa_msgs.JointPositionVelocity._TYPE);
    jv = helper.buildMessage(iiwa_msgs.JointVelocity._TYPE);
  }

  /**
   * Resets all sequence IDs back to 0, so that new commands will be accepted
   */
  public void resetSequenceIds() {
    cp.getHeader().setSeq(0);
    cp_lin.getHeader().setSeq(0);
    cv.getHeader().setSeq(0);
    jp.getHeader().setSeq(0);
    jpv.getHeader().setSeq(0);
    jv.getHeader().setSeq(0);
  }

  /**
   * Add a callback to the SmartServo service
   */
  public void setConfigureControlModeCallback(ServiceResponseBuilder<iiwa_msgs.ConfigureControlModeRequest, iiwa_msgs.ConfigureControlModeResponse> callback) {
    configureControlModeCallback = callback;
  }

  /**
   * Add a callback to the TimeToDestination service
   */
  public void setTimeToDestinationCallback(ServiceResponseBuilder<iiwa_msgs.TimeToDestinationRequest, iiwa_msgs.TimeToDestinationResponse> callback) {
    timeToDestinationCallback = callback;
  }

  /**
   * Add a callback to the SetSpeedOverride service
   */
  public void setSpeedOverrideCallback(
      ServiceResponseBuilder<iiwa_msgs.SetSpeedOverrideRequest, iiwa_msgs.SetSpeedOverrideResponse> callback) {
    setSpeedOverrideCallback = callback;
  }

  /**
   * Add a callback to the SetPTPCartesianSpeedLimits service
   */
  public void setPTPCartesianSpeedLimitsCallback(
      ServiceResponseBuilder<iiwa_msgs.SetPTPCartesianSpeedLimitsRequest, iiwa_msgs.SetPTPCartesianSpeedLimitsResponse> callback) {
    setPTPCartesianSpeedLimitsCallback = callback;
  }

  /**
   * Add a callback to the SetPTPCartesianSpeedLimits service
   */
  public void setPTPJointSpeedLimitsCallback(
      ServiceResponseBuilder<iiwa_msgs.SetPTPJointSpeedLimitsRequest, iiwa_msgs.SetPTPJointSpeedLimitsResponse> callback) {
    setPTPJointSpeedLimitsCallback = callback;
  }

  /**
   * Add a callback to the SetSmartServoJointSpeedLimits service
   */
  public void setSmartServoJointSpeedLimitsCallback(
      ServiceResponseBuilder<iiwa_msgs.SetSmartServoJointSpeedLimitsRequest, iiwa_msgs.SetSmartServoJointSpeedLimitsResponse> callback) {
    setSmartServoJointSpeedLimitsCallback = callback;
  }
  
  /**
   * Add a callback to the SetSmartServoLinSpeedLimits service
   */
  public void setSmartServoLinSpeedLimitsCallback(
      ServiceResponseBuilder<iiwa_msgs.SetSmartServoLinSpeedLimitsRequest, iiwa_msgs.SetSmartServoLinSpeedLimitsResponse> callback) {
    setSmartServoLinSpeedLimitsCallback = callback;
  }

  /**
   * Add a callback to the SetWorkpiece service
   */
  public void setWorkpieceCallback(ServiceResponseBuilder<iiwa_msgs.SetWorkpieceRequest, iiwa_msgs.SetWorkpieceResponse> callback) {
    setWorkpieceCallback = callback;
  }

  /**
   * Add a callback to the SetEndpointFrame service
   */
  public void setEndpointFrameCallback(ServiceResponseBuilder<iiwa_msgs.SetEndpointFrameRequest, iiwa_msgs.SetEndpointFrameResponse> callback) {
    setEndpointFrameCallback = callback;
  }

  /**
   * Returns the last PoseStamped message received from the /command/CartesianPose topic. Returns null if no
   * new message is available.
   * <p>
   * 
   * @return the received PoseStamped message.
   */
  public geometry_msgs.PoseStamped getCartesianPose() {
    synchronized (new_cp) {
      if (new_cp) {
        new_cp = false;
        return cp;
      }
      else {
        return null;
      }
    }
  }

  /**
   * Returns the last PoseStamped message received from the /command/CartesianPoseLin topic. Returns null if
   * no new message is available.
   * <p>
   * 
   * @return the received PoseStamped message.
   */
  public geometry_msgs.PoseStamped getCartesianPoseLin() {
    synchronized (new_cp_lin) {
      if (new_cp_lin) {
        new_cp_lin = false;
        return cp_lin;
      }
      else {
        return null;
      }
    }
  }

  /**
   * TODO
   * 
   * @return the received PoseStamped message.
   */
  public geometry_msgs.TwistStamped getCartesianVelocity() {
    return cv;
  }

  /**
   * Returns the last received Joint Position message. Returns null if no new message is available.
   * <p>
   * 
   * @return the received Joint Position message.
   */
  public iiwa_msgs.JointPosition getJointPosition() {
    synchronized (new_jp) {
      if (new_jp) {
        new_jp = false;
        return jp;
      }
      else {
        return null;
      }
    }
  }

  /**
   * Returns the last received Joint Position-Velocity message. Returns null if no new message is available.
   * <p>
   * 
   * @return the received Joint Position-Velocity message.
   */
  public iiwa_msgs.JointPositionVelocity getJointPositionVelocity() {
    synchronized (new_jpv) {
      if (new_jpv) {
        new_jpv = false;
        return jpv;
      }
      else {
        return null;
      }
    }
  }

  /**
   * Transforms a pose to the given TF reference frame.
   * 
   * @param pose
   * @param tartget_frame
   * @return pose transformed to target_frame
   **/
  public geometry_msgs.PoseStamped transformPose(geometry_msgs.PoseStamped pose, String targetFrame) {

    if (pose == null || pose.getHeader().getFrameId() == null || targetFrame == null) { return null; }
    if (pose.getHeader().getFrameId().equals(targetFrame)) { return pose; }

    long time = pose.getHeader().getStamp().totalNsecs();

    PoseStamped result = helper.buildMessage(PoseStamped._TYPE);
    result.getHeader().setFrameId(targetFrame);
    result.getHeader().setSeq(pose.getHeader().getSeq());
    result.getHeader().setStamp(pose.getHeader().getStamp());

    if (tfListener.getTree().canTransform(pose.getHeader().getFrameId(), targetFrame)) {
      Quaternion q_raw = pose.getPose().getOrientation();
      Point t_raw = pose.getPose().getPosition();

      Quat4d q = new Quat4d(q_raw.getX(), q_raw.getY(), q_raw.getZ(), q_raw.getW());
      Vector3d t = new Vector3d(t_raw.getX(), t_raw.getY(), t_raw.getZ());

      Matrix4d mat = new Matrix4d(q, t, 1);

      Transform transform = tfListener.getTree().lookupTransformBetween(pose.getHeader().getFrameId(), targetFrame, time);

      if (transform == null) { return null; }
      transform.invert();

      Matrix4d transformed = transform.asMatrix();
      transformed.mul(mat);

      Matrix3d base = new Matrix3d(transformed.getM00(), transformed.getM01(), transformed.getM02(), transformed.getM10(), transformed.getM11(), transformed.getM12(),
          transformed.getM20(), transformed.getM21(), transformed.getM22());
      q.set(base);

      result.setPose(helper.getPose(transformed));
    }
    else {
      result.getPose().getOrientation().setW(1);
    }

    return result;
  }

  /**
   * Creates a KUKA Sunrise frame from a CartesianPose message. Includes resolving TF transformation and
   * applying redundancy data.
   * 
   * @param parent: parent frame of robot base coordinate system
   * @param cartesianPose: Pose to transform
   * @param robotBaseFrame: String id of robot base frame (usually iiwa_link_0)
   * @return
   **/
	public Frame cartesianPoseToRosFrame(AbstractFrame parent, CartesianPose cartesianPose, String robotBaseFrame) {
		PoseStamped poseStamped = transformPose(cartesianPose.getPoseStamped(),	robotBaseFrame);
		
		if (poseStamped == null) {
			return null;
		}

		Frame frame = Conversions.rosPoseToKukaFrame(parent, poseStamped.getPose());
		RedundancyInformation redundancy = cartesianPose.getRedundancy();

		if (redundancy.getStatus() >= 0 && redundancy.getTurn() >= 0) {
  		// You can get this info from the robot Cartesian Position (SmartPad)
		  // or the /iiwa/state/CartesianPose topic
			IRedundancyCollection redundantData = new LBRE1Redundancy(
					redundancy.getE1(),
					redundancy.getStatus(),
					redundancy.getTurn()
			);
			frame.setRedundancyInformation(robot, redundantData);
		}

		return frame;
	}

  /**
   * Returns the last received Joint Velocity message. Returns null if no new message is available.
   * <p>
   * 
   * @return the received Joint Velocity message.
   */
  public iiwa_msgs.JointVelocity getJointVelocity() {
    return jv;
  }

  /**
   * @see org.ros.node.NodeMain#getDefaultNodeName()
   */
  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of(iiwaName + "/subscriber");
  }

  /**
   * Checks that the given message headers are in the correct order. That is, new messages should have a
   * larger sequence number than the last one received. True is also returned is both sequence numbers are
   * zero, that means that probably the user is not setting them at all.
   * 
   * @param received_header - the newly received message header we want to compare.
   * @param stored_header - the last received message header that was stored.
   * @return
   */
  private Boolean checkMessageSequence(std_msgs.Header received_header, std_msgs.Header stored_header) {
    return ((received_header.getSeq() == 0 && stored_header.getSeq() == 0) || received_header.getSeq() > stored_header.getSeq());
  }

  /**
   * This method is called when the <i>execute</i> method from a <i>nodeMainExecutor</i> is called.<br>
   * Do <b>NOT</b> manually call this.
   * <p>
   * 
   * @see org.ros.node.AbstractNodeMain#onStart(org.ros.node.ConnectedNode)
   **/
  @Override
  public void onStart(ConnectedNode connectedNode) {

    node = connectedNode;

    // Creating the subscribers
    cartesianPoseSubscriber = connectedNode.newSubscriber(iiwaName + "/command/CartesianPose", geometry_msgs.PoseStamped._TYPE);
    cartesianPoseLinSubscriber = connectedNode.newSubscriber(iiwaName + "/command/CartesianPoseLin", geometry_msgs.PoseStamped._TYPE);
    cartesianVelocitySubscriber = connectedNode.newSubscriber(iiwaName + "/command/CartesianVelocity", geometry_msgs.TwistStamped._TYPE);
    jointPositionSubscriber = connectedNode.newSubscriber(iiwaName + "/command/JointPosition", iiwa_msgs.JointPosition._TYPE);
    jointPositionVelocitySubscriber = connectedNode.newSubscriber(iiwaName + "/command/JointPositionVelocity", iiwa_msgs.JointPositionVelocity._TYPE);
    jointVelocitySubscriber = connectedNode.newSubscriber(iiwaName + "/command/JointVelocity", iiwa_msgs.JointVelocity._TYPE);
    tfListener = new TransformListener(connectedNode);

    // Subscribers' callbacks
    cartesianPoseSubscriber.addMessageListener(new MessageListener<geometry_msgs.PoseStamped>() {
      @Override
      public void onNewMessage(geometry_msgs.PoseStamped position) {
        if (enforceMessageSequence) {
          if (!checkMessageSequence(position.getHeader(), cp.getHeader())) {
            Logger.error("Received a PoseStamped message with the SeqNum " + position.getHeader().getSeq() + " while expecting a SeqNum larger than " + jp.getHeader().getSeq());
            return;
          }
        }
        synchronized (new_cp) {
          cp = position;
          currentCommandType = CommandType.CARTESIAN_POSE;
          new_cp = true;
        }
      }
    });

    cartesianVelocitySubscriber.addMessageListener(new MessageListener<geometry_msgs.TwistStamped>() {
      @Override
      public void onNewMessage(geometry_msgs.TwistStamped velocity) {
        if (enforceMessageSequence) {
          if (!checkMessageSequence(velocity.getHeader(), cv.getHeader())) {
            Logger.error("Received a TwistStamped message with the SeqNum " + velocity.getHeader().getSeq() + ". while expecting a SeqNum larger than " + cv.getHeader().getSeq());
            return;
          }
        }
        cv = velocity;
        currentCommandType = CommandType.CARTESIAN_VELOCITY;
      }
    });

    cartesianPoseLinSubscriber.addMessageListener(new MessageListener<geometry_msgs.PoseStamped>() {
      @Override
      public void onNewMessage(geometry_msgs.PoseStamped position) {
        if (enforceMessageSequence) {
          if (!checkMessageSequence(position.getHeader(), cp_lin.getHeader())) {
            Logger
                .error("Received a PoseStamped message with the SeqNum " + position.getHeader().getSeq() + " while expecting a SeqNum larger than " + cp_lin.getHeader().getSeq());
            return;
          }
        }
        synchronized (new_cp_lin) {
          cp_lin = position;
          currentCommandType = CommandType.CARTESIAN_POSE_LIN;
          new_cp_lin = true;
        }
      }
    });

    jointPositionSubscriber.addMessageListener(new MessageListener<iiwa_msgs.JointPosition>() {
      @Override
      public void onNewMessage(iiwa_msgs.JointPosition position) {
        if (enforceMessageSequence) {
          if (!checkMessageSequence(position.getHeader(), jp.getHeader())) {
            Logger.error("Received a JointPosition message with the SeqNum " + position.getHeader().getSeq() + " while expecting a SeqNum larger than " + jp.getHeader().getSeq());
            return;
          }
        }
        synchronized (new_jp) {
          jp = position;
          currentCommandType = CommandType.JOINT_POSITION;
          new_jp = true;
        }
      }
    });

    jointPositionVelocitySubscriber.addMessageListener(new MessageListener<iiwa_msgs.JointPositionVelocity>() {
      @Override
      public void onNewMessage(iiwa_msgs.JointPositionVelocity positionVelocity) {
        if (enforceMessageSequence) {
          if (!checkMessageSequence(positionVelocity.getHeader(), jpv.getHeader())) {
            Logger.error("Received a JointPositionVelocity message with the SeqNum " + positionVelocity.getHeader().getSeq() + " while expecting a SeqNum larger than "
                + jpv.getHeader().getSeq());
            return;
          }
        }
        synchronized (new_jpv) {
          jpv = positionVelocity;
          currentCommandType = CommandType.JOINT_POSITION_VELOCITY;
          new_jpv = true;
        }
      }
    });

    jointVelocitySubscriber.addMessageListener(new MessageListener<iiwa_msgs.JointVelocity>() {
      @Override
      public void onNewMessage(iiwa_msgs.JointVelocity velocity) {
        if (enforceMessageSequence) {
          if (!checkMessageSequence(velocity.getHeader(), jv.getHeader())) {
            Logger.error("Received a JointVelocity message with the SeqNum " + velocity.getHeader().getSeq() + " while expecting a SeqNum larger than " + jv.getHeader().getSeq());
            return;
          }
        }
        jv = velocity;
        currentCommandType = CommandType.JOINT_VELOCITY;
      }
    });

    // Creating SmartServo service if a callback has been defined.
    if (configureControlModeCallback != null) {
      configureControlModeServer = node.newServiceServer(iiwaName + "/configuration/ConfigureControlMode", "iiwa_msgs/ConfigureControlMode", configureControlModeCallback);
    }

    // Creating TimeToDestination service if a callback has been defined.
    if (timeToDestinationCallback != null) {
      timeToDestinationServer = node.newServiceServer(iiwaName + "/state/timeToDestination", "iiwa_msgs/TimeToDestination", timeToDestinationCallback);
    }
    
    // Creating SpeedOverride service to scale global application speed.
    if (setSpeedOverrideCallback != null) {
      setSpeedOverrideServer = node.newServiceServer(iiwaName + "/configuration/SpeedOverride",
          "iiwa_msgs/SetSpeedOverride", setSpeedOverrideCallback);
    }
    
    // Creating PTPJointSpeedLimits service to set speed limitations for PTP motions in joint space.
    if (setPTPCartesianSpeedLimitsCallback != null) {
      setPTPCartesianSpeedLimitsServer = node.newServiceServer(iiwaName + "/configuration/PTPCartesianSpeedLimits",
          "iiwa_msgs/SetPTPCartesianSpeedLimits", setPTPCartesianSpeedLimitsCallback);
    }
    
    // Creating PTPJointSpeedLimits service to set speed limitations for PTP motions in joint space.
    if (setPTPJointSpeedLimitsCallback != null) {
      setPTPJointSpeedLimitsServer = node.newServiceServer(iiwaName + "/configuration/PTPJointSpeedLimits",
          "iiwa_msgs/SetPTPJointSpeedLimits", setPTPJointSpeedLimitsCallback);
    }

    // Creating SmartServoJointSpeedLimits service to set speed limitations for SmartServo motions in joint space.
    if (setSmartServoJointSpeedLimitsCallback != null) {
      setSmartServoJointSpeedLimitsServer = node.newServiceServer(iiwaName + "/configuration/smartServoJointSpeedLimits",
          "iiwa_msgs/SetSmartServoJointSpeedLimits", setSmartServoJointSpeedLimitsCallback);
    }

    // Creating SmartServoLinSpeedLimits service to set speed limitations for SmartServo motions in Cartesian space.
    if (setSmartServoLinSpeedLimitsCallback != null) {
      setSmartServoLinSpeedLimitsServer = node.newServiceServer(iiwaName + "/configuration/smartServoLinSpeedLimits",
          "iiwa_msgs/SetSmartServoLinSpeedLimits", setSmartServoLinSpeedLimitsCallback);
    }

    // Creating TimeToDestination service if a callback has been defined.
    if (setWorkpieceCallback != null) {
      setWorkpieceServer = node.newServiceServer(iiwaName + "/configuration/setWorkpiece", "iiwa_msgs/SetWorkpiece", setWorkpieceCallback);
    }

    // Creating TimeToDestination service if a callback has been defined.
    if (setEndpointFrameCallback != null) {
      setEndpointFrameServer = node.newServiceServer(iiwaName + "/configuration/setEndpointFrame", "iiwa_msgs/SetEndpointFrame", setEndpointFrameCallback);
    }
  }
}
