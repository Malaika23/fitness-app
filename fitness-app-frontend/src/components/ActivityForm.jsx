// Empty file
import { Box, Button, FormControl, InputLabel, MenuItem, Select, TextField } from '@mui/material'
import React, { useState } from 'react'
import { addActivity } from '../services/api'


const ActivityForm = ({ onActivityAdded, onActivitiesAdded }) => {

    const [activity, setActivity] = useState({
        type: "RUNNING", duration: '', caloriesBurned: '',
        additionalMetrics: {}
    });
    const [customType, setCustomType] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const payload = {
                ...activity,
                additionalMetrics: activity.type === "OTHER" ? { ...activity.additionalMetrics, customType } : activity.additionalMetrics
            };
            await addActivity(payload);
            const callback = onActivityAdded || onActivitiesAdded;
            if (callback) {
                callback();
            }
            setActivity({ type: "RUNNING", duration: '', caloriesBurned: '', additionalMetrics: {} });
            setCustomType('');
        } catch (error) {
            console.error(error);
        }
    }

    return (
        <Box component="form" onSubmit={handleSubmit} sx={{ mb: 4 }}>
            <FormControl fullWidth sx={{ mb: 2 }}>
                <InputLabel>Activity Type</InputLabel>
                <Select
                    value={activity.type}
                    onChange={(e) => setActivity({ ...activity, type: e.target.value })}>
                    <MenuItem value="RUNNING">Running</MenuItem>
                    <MenuItem value="WALKING">Walking</MenuItem>
                    <MenuItem value="CYCLING">Cycling</MenuItem>
                    <MenuItem value="SWIMMING">Swimming</MenuItem>
                    <MenuItem value="YOGA">Yoga</MenuItem>
                    <MenuItem value="STRENGTH_TRAINING">Strength Training</MenuItem>
                    <MenuItem value="HIIT">HIIT</MenuItem>
                    <MenuItem value="PILATES">Pilates</MenuItem>
                    <MenuItem value="HIKING">Hiking</MenuItem>
                    <MenuItem value="DANCING">Dancing</MenuItem>
                    <MenuItem value="ROWING">Rowing</MenuItem>
                    <MenuItem value="ELLIPTICAL">Elliptical</MenuItem>
                    <MenuItem value="CARDIO">Cardio</MenuItem>
                    <MenuItem value="STRETCHING">Stretching</MenuItem>
                    <MenuItem value="OTHER">Other</MenuItem>
                </Select>
            </FormControl>

            {activity.type === "OTHER" && (
                <TextField
                    fullWidth
                    label="Custom Activity Name"
                    sx={{ mb: 2 }}
                    value={customType}
                    onChange={(e) => setCustomType(e.target.value)}
                    required
                />
            )}

            <TextField fullWidth
                label="Duration (Minutes)"
                type='number'
                sx={{ mb: 2 }}
                value={activity.duration}
                onChange={(e) => setActivity({ ...activity, duration: e.target.value })} />

            <TextField fullWidth
                label="Calories Burned"
                type='number'
                sx={{ mb: 2 }}
                value={activity.caloriesBurned}
                onChange={(e) => setActivity({ ...activity, caloriesBurned: e.target.value })} />

            <Button type='submit' variant='contained'>
                Add Activity
            </Button>
        </Box>
    )
}

export default ActivityForm